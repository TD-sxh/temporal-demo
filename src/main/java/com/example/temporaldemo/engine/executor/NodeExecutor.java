package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.model.NodeDefinition;

/**
 * Interface for node executors. Each {@link com.example.temporaldemo.engine.model.NodeType}
 * has a corresponding executor implementation.
 *
 * <p>The executor processes the node and returns the ID of the next node
 * to execute (or {@code null} if the workflow should end).
 */
public interface NodeExecutor {

    /**
     * Execute the given node.
     *
     * @param node    the node definition
     * @param context the current workflow context (read/write)
     * @return the ID of the next node to transition to, or null to end
     */
    String execute(NodeDefinition node, WorkflowContext context);
}
