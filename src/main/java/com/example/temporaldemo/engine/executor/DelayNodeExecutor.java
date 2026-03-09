package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.model.NodeDefinition;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Executor for DELAY nodes.
 *
 * <p>Pauses the workflow for the specified duration using
 * {@link Workflow#sleep(Duration)}, which is timer-based and
 * replay-safe.
 */
public class DelayNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(DelayNodeExecutor.class);

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        int seconds = node.getDelaySeconds();
        logger.info("DELAY [{}]: sleeping for {} seconds", node.getId(), seconds);
        Workflow.sleep(Duration.ofSeconds(seconds));
        logger.info("DELAY [{}]: woke up after {} seconds", node.getId(), seconds);
        return node.getNext();
    }
}
