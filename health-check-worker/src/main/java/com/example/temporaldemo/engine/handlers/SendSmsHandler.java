package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: sends an SMS notification to the doctor.
 * Registered name: "sendSms"
 */
public class SendSmsHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(SendSmsHandler.class);

    @Override
    public Object handle(Map<String, Object> input) {
        String doctorName = (String) input.get("doctorName");
        String patientId = (String) input.get("patientId");
        String severity = (String) input.get("severity");

        logger.info("[sendSms] Sending SMS to Dr. {} — patient {} severity: {}", doctorName, patientId, severity);

        // Simulate SMS send latency
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return Map.of(
                "channel", "SMS",
                "recipient", doctorName,
                "status", "SENT",
                "message", "ALERT: Patient " + patientId + " — " + severity
        );
    }
}
