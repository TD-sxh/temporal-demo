package com.example.temporaldemo.engine.digitalmessage;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * Child workflow for digital message coordination.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Send notifications in parallel via configured channels (EMAIL / SMS / PHONE).</li>
 *   <li>Wait for a {@link #digitalMessageSignal(String)} or timeout.</li>
 *   <li>Return a {@link DigitalMessageResult} to the parent workflow.</li>
 * </ol>
 *
 * <p>Workflow ID convention (set by {@code DigitalMessageNodeExecutor}):
 * <pre>{parentWorkflowId}__digitalMessage__{nodeId}</pre>
 */
@WorkflowInterface
public interface DigitalMessageWorkflow {

    @WorkflowMethod
    DigitalMessageResult execute(DigitalMessageRequest request);

    /**
     * Send an action to resolve the waiting digital message node.
     *
     * <p>Supported actions (case-insensitive):
     * <ul>
     *   <li>{@code execute}   — approve and continue parent workflow</li>
     *   <li>{@code skip}      — skip this node, parent workflow continues</li>
     *   <li>{@code terminate} — terminate the parent workflow</li>
     *   <li>{@code cancel}    — cancel the digital message and stop the workflow</li>
     * </ul>
     */
    @SignalMethod
    void digitalMessageSignal(String action);

    /** Pause the digital message wait (timeout countdown suspended). */
    @SignalMethod
    void pauseDigitalMessage();

    /** Resume a previously paused digital message wait. */
    @SignalMethod
    void resumeDigitalMessage();

    @QueryMethod
    Map<String, Object> getTaskStatus();
}
