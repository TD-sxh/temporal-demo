package com.example.temporaldemo.engine.humantask;

import java.util.List;

/**
 * Input contract for {@code HumanTaskWorkflow}.
 *
 * <p>Serialised into the child workflow's input, so all fields must be
 * JSON-serialisable via Jackson defaults.
 */
public class HumanTaskRequest {

    /** ID of the HUMAN_TASK node in the parent workflow definition. */
    private String taskId;

    /** Human-readable node name (used in notification body). */
    private String taskName;

    /** Parent workflow ID — for correlation and logging. */
    private String parentWorkflowId;

    /**
     * Notification channels to activate.
     * Supported values: {@code "EMAIL"}, {@code "SMS"}, {@code "PHONE"}.
     */
    private List<String> channels;

    /** Recipient email address (EMAIL channel). */
    private String to;

    /** Recipient phone number (SMS / PHONE channels). */
    private String phone;

    /** Notification message body. */
    private String message;

    /**
     * Wait timeout in seconds before auto-approval.
     * Defaults to {@code 3600} when 0 or negative.
     */
    private int timeoutSeconds;

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getParentWorkflowId() { return parentWorkflowId; }
    public void setParentWorkflowId(String parentWorkflowId) { this.parentWorkflowId = parentWorkflowId; }

    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
