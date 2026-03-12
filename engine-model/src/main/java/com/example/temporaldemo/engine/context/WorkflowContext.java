package com.example.temporaldemo.engine.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runtime context for a running orchestrator workflow.
 *
 * <p>Holds all workflow variables (initial + accumulated), node outputs,
 * and pending signals. Passed through every node executor so nodes
 * can read/write shared state.
 *
 * <p>This object is entirely deterministic — it only stores simple types
 * that Temporal can serialize via Jackson. Must NOT hold references to
 * threads, connections, or I/O handles.
 */
public class WorkflowContext {

    /** All workflow variables (initial + set by nodes). Key = variable name. */
    private final Map<String, Object> variables = new LinkedHashMap<>();

    /** Node outputs keyed by outputKey. Subset of variables specifically from TASK nodes. */
    private final Map<String, Object> nodeOutputs = new LinkedHashMap<>();

    /** Pending signals keyed by signal name → Queue of payloads */
    private final Map<String, ConcurrentLinkedQueue<Object>> pendingSignals = new LinkedHashMap<>();

    /** ID of the currently executing node (for diagnostics / Query) */
    private String currentNodeId;

    /** Overall status description */
    private String statusMessage = "RUNNING";

    /** Per-node execution history — entries are mutable maps updated in-place */
    private final List<Map<String, Object>> nodeHistory = new ArrayList<>();

    // ─── Variables ──────────────────────────────────

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object val = variables.get(key);
        if (val == null) return null;
        if (type.isInstance(val)) return (T) val;
        // Handle numeric conversions (Jackson may deserialize numbers as Integer/Double)
        if (type == Integer.class && val instanceof Number) return (T) Integer.valueOf(((Number) val).intValue());
        if (type == Long.class && val instanceof Number) return (T) Long.valueOf(((Number) val).longValue());
        if (type == Double.class && val instanceof Number) return (T) Double.valueOf(((Number) val).doubleValue());
        if (type == String.class) return (T) val.toString();
        return (T) val;
    }

    public Map<String, Object> getAllVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public void putAllVariables(Map<String, Object> vars) {
        if (vars != null) {
            variables.putAll(vars);
        }
    }

    // ─── Node Outputs ───────────────────────────────

    public void setNodeOutput(String outputKey, Object value) {
        nodeOutputs.put(outputKey, value);
        variables.put(outputKey, value); // also make it visible as a variable
    }

    public Object getNodeOutput(String outputKey) {
        return nodeOutputs.get(outputKey);
    }

    public Map<String, Object> getAllNodeOutputs() {
        return Collections.unmodifiableMap(nodeOutputs);
    }

    // ─── Signals ────────────────────────────────────

    /**
     * Enqueue a signal payload (called from Signal handler).
     */
    public void addSignal(String signalName, Object payload) {
        pendingSignals.computeIfAbsent(signalName, k -> new ConcurrentLinkedQueue<>()).add(payload);
    }

    /**
     * Poll one pending signal payload, or null if none.
     */
    public Object pollSignal(String signalName) {
        ConcurrentLinkedQueue<Object> queue = pendingSignals.get(signalName);
        return queue == null ? null : queue.poll();
    }

    /**
     * Check if there is at least one pending signal with the given name.
     */
    public boolean hasSignal(String signalName) {
        ConcurrentLinkedQueue<Object> queue = pendingSignals.get(signalName);
        return queue != null && !queue.isEmpty();
    }

    // ─── Node History ────────────────────────────────

    /**
     * Add a new node execution entry. Returns the entry so the caller can mutate it later.
     */
    public Map<String, Object> startNodeHistory(String nodeId, String nodeType, long startedAt) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("nodeId", nodeId);
        entry.put("nodeType", nodeType);
        entry.put("status", "EXECUTING");
        entry.put("startedAt", startedAt);
        nodeHistory.add(entry);
        return entry;
    }

    public List<Map<String, Object>> getNodeHistory() {
        return Collections.unmodifiableList(nodeHistory);
    }

    // ─── Status / Diagnostics ───────────────────────

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    @Override
    public String toString() {
        return "WorkflowContext{" +
                "currentNode='" + currentNodeId + '\'' +
                ", status='" + statusMessage + '\'' +
                ", variables=" + variables +
                '}';
    }
}
