package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: sends an email notification to the doctor.
 * Registered name: "sendEmail"
 */
public class SendEmailHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(SendEmailHandler.class);

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(Map<String, Object> input) {
        String doctorName = (String) input.get("doctorName");
        String patientId = (String) input.get("patientId");
        Object diagnosisObj = input.get("diagnosis");

        String description = "N/A";
        if (diagnosisObj instanceof Map) {
            Map<String, Object> diagnosis = (Map<String, Object>) diagnosisObj;
            description = (String) diagnosis.getOrDefault("description", "N/A");
        }

        logger.info("[sendEmail] Sending email to Dr. {} about patient {} — {}", doctorName, patientId, description);

        // Simulate email send latency
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return Map.of(
                "channel", "EMAIL",
                "recipient", doctorName,
                "status", "SENT",
                "message", "Urgent: Patient " + patientId + " diagnosis — " + description
        );
    }
}
