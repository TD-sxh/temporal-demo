package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.humantask.HumanTaskRequest;
import com.example.temporaldemo.engine.humantask.HumanTaskResult;
import com.example.temporaldemo.engine.humantask.HumanTaskWorkflow;
import com.example.temporaldemo.engine.model.HumanTaskConfig;
import com.example.temporaldemo.engine.model.NodeDefinition;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

/**
 * Executor for HUMAN_TASK nodes.
 *
 * <p>Delegates all notification and waiting logic to a {@link HumanTaskWorkflow}
 * child workflow. The parent workflow simply blocks on
 * {@code childStub.execute(request)} until the child completes.
 *
 * <p>Child workflow ID convention:
 * <pre>{parentWorkflowId}__humanTask__{nodeId}</pre>
 *
 * <p>workflow-admin discovers this ID via the parent's {@code getStatus()} query
 * ({@code pendingHumanTaskId} field) and sends {@code humanTaskSignal} directly
 * to the child workflow.
 */
public class HumanTaskNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(HumanTaskNodeExecutor.class);

    /**
     * Supplies the parent workflow's current pending human-task child workflow ID.
     * Written by this executor before blocking, consumed by {@code getStatus()}.
     */
    private final java.util.function.Consumer<String> setPendingHumanTaskId;

    public HumanTaskNodeExecutor(java.util.function.Consumer<String> setPendingHumanTaskId) {
        this.setPendingHumanTaskId = setPendingHumanTaskId;
    }

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        logger.info("HUMAN_TASK [{}]: launching child workflow", node.getId());

        HumanTaskConfig cfg = node.getHumanTask();

        // ── Build child workflow ID ──────────────────────────────────────
        String parentId = Workflow.getInfo().getWorkflowId();
        String childWorkflowId = parentId + "__humanTask__" + node.getId();

        // ── Build request ────────────────────────────────────────────────
        HumanTaskRequest request = new HumanTaskRequest();
        request.setTaskId(node.getId());
        request.setTaskName(node.getName() != null ? node.getName() : node.getId());
        request.setParentWorkflowId(parentId);
        if (cfg != null) {
            request.setChannels(cfg.getChannels());
            request.setTo(cfg.getTo());
            request.setPhone(cfg.getPhone());
            request.setMessage(cfg.getMessage());
            request.setTimeoutSeconds(cfg.getTimeoutSeconds());
        }

        // ── Expose child workflow ID in parent status ────────────────────
        context.setStatusMessage("WAITING_HUMAN_TASK: " + node.getId());
        setPendingHumanTaskId.accept(childWorkflowId);

        // ── Launch child workflow and block until it completes ───────────
        HumanTaskWorkflow childStub = Workflow.newChildWorkflowStub(
                HumanTaskWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(childWorkflowId)
                        .build());

        HumanTaskResult result = childStub.execute(request);

        // ── Clear pending task ID after resolution ───────────────────────
        setPendingHumanTaskId.accept(null);
        context.setNodeOutput(node.getId() + "_result", result.getAction());

        logger.info("HUMAN_TASK [{}]: resolved with action='{}'", node.getId(), result.getAction());

        return switch (result.getAction()) {
            case "TERMINATED" -> {
                context.setStatusMessage("TERMINATED");
                yield null;   // null ends the parent workflow loop
            }
            default -> node.getNext();
        };
    }
}
