package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.model.NodeDefinition;
import com.example.temporaldemo.engine.model.NodeType;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

/**
 * Executor for START and END nodes.
 *
 * <p>Both are no-op pass-through nodes:
 * <ul>
 *   <li><b>START</b>: transitions to {@code next} (the first real node)</li>
 *   <li><b>END</b>: returns {@code null} to terminate the workflow</li>
 * </ul>
 *
 * <p>These nodes exist so the workflow JSON is self-describing — the
 * structure contains explicit entry/exit points rather than relying on
 * conventions like "whichever node has no incoming edges is the start".
 */
public class StartEndNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(StartEndNodeExecutor.class);

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        if (node.getType() == NodeType.END) {
            logger.info("Reached END node: {}", node.getId());
            context.setStatusMessage("COMPLETED");
            return null; // terminates the workflow walk
        }

        // START: just pass through to next
        logger.info("START node: {}, proceeding to next: {}", node.getId(), node.getNext());
        return node.getNext();
    }
}
