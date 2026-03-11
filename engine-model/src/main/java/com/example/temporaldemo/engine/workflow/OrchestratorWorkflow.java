package com.example.temporaldemo.engine.workflow;

import com.example.temporaldemo.engine.context.WorkflowContext;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * Generic orchestrator workflow interface.
 *
 * <p>This workflow takes a JSON workflow definition (as String) and executes
 * it, walking through nodes according to the graph. All business logic
 * is defined in JSON — this workflow is a pure interpreter.
 *
 * <p>Supports generic signals (key-value pairs) and queries for status.
 */
@WorkflowInterface
public interface OrchestratorWorkflow {

    /**
     * Execute the workflow defined by the given JSON definition string.
     *
     * @param workflowDefinitionJson the JSON workflow definition
     * @param inputVariables         initial variables to inject into the context
     * @return final workflow context variables as a Map
     */
    @WorkflowMethod
    Map<String, Object> execute(String workflowDefinitionJson, Map<String, Object> inputVariables);

    /**
     * Generic signal: deliver a named signal with a payload.
     *
     * <p>This can be used by WAIT nodes to unblock when a specific signal arrives.
     *
     * @param signalName the signal name (must match a WAIT node's signalName)
     * @param payload    the signal data (stored in context under the WAIT node's outputKey)
     */
    @SignalMethod
    void signal(String signalName, Object payload);

    /**
     * Pause the workflow. Execution halts after the current node completes.
     * Send a {@link #resume()} signal to continue.
     */
    @SignalMethod
    void pause();

    /**
     * Resume a paused workflow.
     */
    @SignalMethod
    void resume();

    /**
     * Query the current workflow status.
     *
     * @return a snapshot including current node, variables, paused state,
     *         and {@code pendingHumanTaskId} when a HUMAN_TASK node is waiting
     */
    @QueryMethod
    Map<String, Object> getStatus();
}
