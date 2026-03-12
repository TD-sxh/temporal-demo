package com.example.temporaldemo.engine.model;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a DIGITAL_MESSAGE node.
 *
 * <p>JSON example:
 * <pre>
 * {
 *   "id": "doctor_approval",
 *   "type": "DIGITAL_MESSAGE",
 *   "digitalMessage": {
 *     "channels": ["EMAIL", "SMS"],
 *     "emailConfig": { "fromEmail": "noreply@hospital.com", "cc": "...", "message": "..." },
 *     "smsConfig":   { "fromPhone": "+1234567890", "message": "..." },
 *     "timeoutSeconds": 120,
 *     "responseStrategy": "ALL",
 *     "input": { "toEmail": "#doctorEmail", "toPhone": "#doctorPhone" },
 *     "outputKey": {}
 *   },
 *   "next": "after_approval"
 * }
 * </pre>
 *
 * <p>Supported actions via signal: execute, skip, terminate.
 */
public class DigitalMessageConfig {

    /** Notification channels: "EMAIL", "SMS", "PHONE". */
    private List<String> channels;

    /** Per-channel email configuration. */
    private EmailConfig emailConfig;

    /** Per-channel SMS configuration. */
    private SmsConfig smsConfig;

    /**
     * Structured timeout before auto-approval.
     * JSON format: {@code {"value": 2, "unit": "minutes"}}
     */
    private TimeoutConfig timeout;

    /** Response strategy: ALL (wait for all channels) or FIRST (first response wins). */
    private String responseStrategy;

    /**
     * Runtime input variables resolved from workflow context (SpEL expressions).
     * E.g. {"toEmail": "#doctorEmail", "toPhone": "#doctorPhone"}
     */
    private Map<String, Object> input;

    /** Output key mapping for storing the result into the workflow context. */
    private Map<String, String> outputKey;

    // --- Getters and Setters ---

    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }

    public EmailConfig getEmailConfig() { return emailConfig; }
    public void setEmailConfig(EmailConfig emailConfig) { this.emailConfig = emailConfig; }

    public SmsConfig getSmsConfig() { return smsConfig; }
    public void setSmsConfig(SmsConfig smsConfig) { this.smsConfig = smsConfig; }

    public TimeoutConfig getTimeout() { return timeout; }
    public void setTimeout(TimeoutConfig timeout) { this.timeout = timeout; }

    /** Convenience: converts structured timeout to seconds (0 = use default). */
    public int getTimeoutSeconds() { return timeout != null ? timeout.toSeconds() : 0; }

    public String getResponseStrategy() { return responseStrategy; }
    public void setResponseStrategy(String responseStrategy) { this.responseStrategy = responseStrategy; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, String> getOutputKey() { return outputKey; }
    public void setOutputKey(Map<String, String> outputKey) { this.outputKey = outputKey; }
}
