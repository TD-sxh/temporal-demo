package com.example.temporaldemo.engine.humantask;

/**
 * Result returned by {@code HumanTaskWorkflow} to the parent workflow.
 */
public class HumanTaskResult {

    /**
     * The action that resolved the human task.
     * One of: {@code EXECUTED}, {@code SKIPPED}, {@code TERMINATED},
     * {@code TIMEOUT_AUTO_APPROVED}.
     */
    private String action;

    /** Human-readable summary message. */
    private String message;

    public HumanTaskResult() {}

    public HumanTaskResult(String action, String message) {
        this.action = action;
        this.message = message;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
