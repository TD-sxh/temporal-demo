package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.digitalmessage.DigitalMessageRequest;
import com.example.temporaldemo.engine.digitalmessage.DigitalMessageResult;
import com.example.temporaldemo.engine.digitalmessage.DigitalMessageWorkflow;
import com.example.temporaldemo.engine.model.DigitalMessageConfig;
import com.example.temporaldemo.engine.model.NodeDefinition;
import com.example.temporaldemo.engine.expression.SpelEvaluator;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Executor for DIGITAL_MESSAGE nodes.
 *
 * <p>Delegates notification and waiting logic to a {@link DigitalMessageWorkflow}
 * child workflow. The parent blocks on {@code childStub.execute(request)} until the
 * child completes.
 *
 * <p>Child workflow ID convention:
 * <pre>{parentWorkflowId}__digitalMessage__{nodeId}</pre>
 */
public class DigitalMessageNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(DigitalMessageNodeExecutor.class);

    private final Consumer<String> setPendingId;

    public DigitalMessageNodeExecutor(Consumer<String> setPendingId) {
        this.setPendingId = setPendingId;
    }

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        logger.info("DIGITAL_MESSAGE [{}]: launching child workflow", node.getId());

        DigitalMessageConfig cfg = node.getDigitalMessage();

        // ── Build child workflow ID ──────────────────────────────────────
        String parentId = Workflow.getInfo().getWorkflowId();
        String childWorkflowId = parentId + "__digitalMessage__" + node.getId();

        // ── Build request ────────────────────────────────────────────────
        DigitalMessageRequest request = new DigitalMessageRequest();
        request.setTaskId(node.getId());
        request.setTaskName(node.getName() != null ? node.getName() : node.getId());
        request.setParentWorkflowId(parentId);

        if (cfg != null) {
            request.setChannels(cfg.getChannels());
            request.setTimeoutSeconds(cfg.getTimeoutSeconds());
            request.setResponseStrategy(cfg.getResponseStrategy());
            request.setEmailConfig(cfg.getEmailConfig());
            request.setSmsConfig(cfg.getSmsConfig());

            // Resolve SpEL expressions in the input map (e.g. #doctorEmail, #doctorPhone)
            Map<String, Object> resolvedInput = SpelEvaluator.resolveInputs(cfg.getInput(), context);

            Object toEmail = resolvedInput.get("toEmail");
            request.setTo(toEmail != null ? toEmail.toString()
                    : (cfg.getEmailConfig() != null ? cfg.getEmailConfig().getCc() : null));

            Object toPhone = resolvedInput.get("toPhone");
            request.setPhone(toPhone != null ? toPhone.toString()
                    : (cfg.getSmsConfig() != null ? cfg.getSmsConfig().getFromPhone() : null));

            // Default message fallback
            if (cfg.getEmailConfig() != null && cfg.getEmailConfig().getMessage() != null) {
                request.setMessage(cfg.getEmailConfig().getMessage());
            } else if (cfg.getSmsConfig() != null) {
                request.setMessage(cfg.getSmsConfig().getMessage());
            }
        }

        // ── Expose child workflow ID in parent status ────────────────────
        context.setStatusMessage("WAITING_DIGITAL_MESSAGE: " + node.getId());
        setPendingId.accept(childWorkflowId);

        // ── Launch child workflow and block until it completes ───────────
        DigitalMessageWorkflow childStub = Workflow.newChildWorkflowStub(
                DigitalMessageWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(childWorkflowId)
                        .build());

        DigitalMessageResult result = childStub.execute(request);

        // ── Clear pending ID after resolution ────────────────────────────
        setPendingId.accept(null);

        // Store result using outputKey mapping (like TASK nodes)
        if (cfg != null && cfg.getOutputKey() != null && !cfg.getOutputKey().isEmpty()) {
            node.setOutputKey(cfg.getOutputKey());
            TaskNodeExecutor.storeOutput(node, context,
                    Map.of("action", result.getAction(), "message", result.getMessage()));
        } else {
            context.setNodeOutput(node.getId() + "_result", result.getAction());
        }

        logger.info("DIGITAL_MESSAGE [{}]: resolved with action='{}'", node.getId(), result.getAction());

        return switch (result.getAction()) {
            case "TERMINATED", "CANCELED" -> {
                context.setStatusMessage(result.getAction());
                yield null;
            }
            default -> node.getNext();
        };
    }
}
