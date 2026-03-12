package com.example.temporaldemo.engine.digitalmessage;

/** Result returned by {@code DigitalMessageWorkflow} to the parent workflow. */
public class DigitalMessageResult {

    private String action;
    private String message;

    public DigitalMessageResult() {}

    public DigitalMessageResult(String action, String message) {
        this.action = action;
        this.message = message;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
