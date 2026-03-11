package com.example.temporaldemo.engine.humantask;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Child workflow for human task coordination.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Send notifications in parallel via configured channels (EMAIL / SMS / PHONE).</li>
 *   <li>Wait for a {@link #humanTaskSignal(String)} or timeout.</li>
 *   <li>Return a {@link HumanTaskResult} to the parent workflow.</li>
 * </ol>
 *
 * <p>Workflow ID convention (set by {@code HumanTaskNodeExecutor}):
 * <pre>{parentWorkflowId}__humanTask__{nodeId}</pre>
 *
 * This allows workflow-admin to discover the child workflow ID from the parent's
 * {@code getStatus()} query ({@code pendingHumanTaskId} field) and send signals
 * directly to it.
 */
@WorkflowInterface
public interface HumanTaskWorkflow {

    /**
     * Execute the human task: notify all configured channels and wait for
     * a manual action or timeout.
     *
     * @param request task configuration and notification details
     * @return the outcome of the task
     */
    @WorkflowMethod
    HumanTaskResult execute(HumanTaskRequest request);

    /**
     * Send an action to resolve the waiting human task.
     *
     * <p>Supported actions (case-insensitive):
     * <ul>
     *   <li>{@code execute}   — immediately approve and continue parent workflow</li>
     *   <li>{@code skip}      — skip this node, parent workflow continues</li>
     *   <li>{@code terminate} — terminate the parent workflow</li>
     * </ul>
     *
     * @param action the resolution action
     */
    @SignalMethod
    void humanTaskSignal(String action);

    /**
     * Query the current state of this human task.
     *
     * @return a map containing {@code taskId}, {@code status}, {@code parentWorkflowId}
     */
    @QueryMethod
    java.util.Map<String, Object> getTaskStatus();
}
