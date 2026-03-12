package com.example.temporaldemo.engine.model;

/** Email channel configuration for a DIGITAL_MESSAGE node. */
public class EmailConfig {

    private String fromEmail;
    private String cc;
    private String bcc;
    private String message;

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public String getCc() { return cc; }
    public void setCc(String cc) { this.cc = cc; }

    public String getBcc() { return bcc; }
    public void setBcc(String bcc) { this.bcc = bcc; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
