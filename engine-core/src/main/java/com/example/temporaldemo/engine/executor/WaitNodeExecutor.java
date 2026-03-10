package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.model.NodeDefinition;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Executor for WAIT nodes.
 *
 * <p>Blocks the workflow until a signal with the specified name arrives,
 * or until the timeout expires.
 *
 * <p>Uses {@link Workflow#await(Duration, java.util.function.Supplier)} so
 * the Temporal Worker thread is released while waiting.
 */
public class WaitNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(WaitNodeExecutor.class);

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        String signalName = node.getSignalName();
        int timeoutSeconds = node.getTimeoutSeconds();
        logger.info("WAIT [{}]: waiting for signal '{}' (timeout={}s)", node.getId(), signalName, timeoutSeconds);

        boolean signalArrived;
        if (timeoutSeconds > 0) {
            signalArrived = Workflow.await(
                    Duration.ofSeconds(timeoutSeconds),
                    () -> context.hasSignal(signalName));
        } else {
            // Wait indefinitely for the signal
            Workflow.await(() -> context.hasSignal(signalName));
            signalArrived = true;
        }

        if (signalArrived) {
            Object payload = context.pollSignal(signalName);
            logger.info("WAIT [{}]: signal '{}' received with payload: {}", node.getId(), signalName, payload);

            // Reuse the same output-storage logic as TASK nodes (supports String & Map outputKey)
            TaskNodeExecutor.storeOutput(node, context, payload);
            return node.getNext();
        } else {
            logger.info("WAIT [{}]: timeout reached for signal '{}'", node.getId(), signalName);
            return node.getTimeoutNext() != null ? node.getTimeoutNext() : node.getNext();
        }
    }
}
