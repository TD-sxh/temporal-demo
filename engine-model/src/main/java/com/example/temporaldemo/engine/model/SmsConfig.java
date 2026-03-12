package com.example.temporaldemo.engine.model;

/** SMS channel configuration for a DIGITAL_MESSAGE node. */
public class SmsConfig {

    private String fromPhone;
    private String message;

    public String getFromPhone() { return fromPhone; }
    public void setFromPhone(String fromPhone) { this.fromPhone = fromPhone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
