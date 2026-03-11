package com.example.temporaldemo.engine.model;

import java.util.List;

/**
 * Configuration for a HUMAN_TASK node.
 *
 * <p>JSON example:
 * <pre>
 * {
 *   "id": "doctor_approval",
 *   "type": "HUMAN_TASK",
 *   "humanTask": {
 *     "channels": ["EMAIL", "SMS", "PHONE"],
 *     "to": "doctor@hospital.com",
 *     "phone": "+1234567890",
 *     "message": "Please review and approve the patient case.",
 *     "timeoutSeconds": 300
 *   },
 *   "next": "after_approval"
 * }
 * </pre>
 *
 * <p>Supported actions via signal:
 * <ul>
 *   <li>{@code execute} — immediately approve and continue workflow</li>
 *   <li>{@code skip}    — skip this node and continue workflow</li>
 *   <li>{@code terminate} — end the workflow</li>
 * </ul>
 *
 * <p>On timeout the node auto-approves (same as {@code execute}).
 */
public class HumanTaskConfig {

    /**
     * Notification channels to use. Supported values: "EMAIL", "SMS", "PHONE".
     * All selected channels are notified in parallel.
     */
    private List<String> channels;

    /** Recipient email address (used for EMAIL channel). */
    private String to;

    /** Recipient phone number (used for SMS and PHONE channels). */
    private String phone;

    /** Notification message body. */
    private String message;

    /**
     * Timeout in seconds. If no signal is received within this duration,
     * the node auto-approves. Defaults to 3600 (1 hour) when 0 or unset.
     */
    private int timeoutSeconds;

    // --- Getters and Setters ---

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
