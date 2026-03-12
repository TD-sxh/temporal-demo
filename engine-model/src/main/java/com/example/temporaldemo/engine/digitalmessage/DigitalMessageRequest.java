package com.example.temporaldemo.engine.digitalmessage;

import com.example.temporaldemo.engine.model.EmailConfig;
import com.example.temporaldemo.engine.model.SmsConfig;

import java.util.List;

/** Input contract for {@code DigitalMessageWorkflow}. */
public class DigitalMessageRequest {

    private String taskId;
    private String taskName;
    private String parentWorkflowId;
    private List<String> channels;

    /** Resolved recipient email address (from SpEL input.toEmail). */
    private String to;

    /** Resolved recipient phone number (from SpEL input.toPhone). */
    private String phone;

    /** Fallback message body (if no per-channel message is set). */
    private String message;

    private int timeoutSeconds;
    private String responseStrategy;

    /** Per-channel email configuration (fromEmail, cc, bcc, message). */
    private EmailConfig emailConfig;

    /** Per-channel SMS configuration (fromPhone, message). */
    private SmsConfig smsConfig;

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

    public String getResponseStrategy() { return responseStrategy; }
    public void setResponseStrategy(String responseStrategy) { this.responseStrategy = responseStrategy; }

    public EmailConfig getEmailConfig() { return emailConfig; }
    public void setEmailConfig(EmailConfig emailConfig) { this.emailConfig = emailConfig; }

    public SmsConfig getSmsConfig() { return smsConfig; }
    public void setSmsConfig(SmsConfig smsConfig) { this.smsConfig = smsConfig; }
}
