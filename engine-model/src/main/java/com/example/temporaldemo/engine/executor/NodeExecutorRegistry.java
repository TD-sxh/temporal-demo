package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.model.NodeType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry mapping {@link NodeType} to the appropriate {@link NodeExecutor}.
 *
 * <p>Initialized once when the workflow starts and used throughout execution.
 */
public class NodeExecutorRegistry {

    private final Map<NodeType, NodeExecutor> executors = new EnumMap<>(NodeType.class);

    public void register(NodeType type, NodeExecutor executor) {
        executors.put(type, executor);
    }

    public NodeExecutor getExecutor(NodeType type) {
        NodeExecutor executor = executors.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for node type: " + type);
        }
        return executor;
    }
}
