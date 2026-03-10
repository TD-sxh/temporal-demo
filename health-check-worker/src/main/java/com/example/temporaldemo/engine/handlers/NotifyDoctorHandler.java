package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: notifies a doctor about a severe case.
 * Registered name: "notifyDoctor"
 *
 * <p>Returns "NOTIFIED"
 */
public class NotifyDoctorHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotifyDoctorHandler.class);

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(Map<String, Object> input) {
        String doctorName = (String) input.get("doctorName");
        String patientId = (String) input.get("patientId");
        Object diagnosisObj = input.get("diagnosis");

        String description = "N/A";
        String score = "N/A";
        if (diagnosisObj instanceof Map) {
            Map<String, Object> diagnosis = (Map<String, Object>) diagnosisObj;
            description = (String) diagnosis.getOrDefault("description", "N/A");
            Object s = diagnosis.get("score");
            score = s != null ? String.format("%.2f", ((Number) s).doubleValue()) : "N/A";
        }

        logger.info("[notifyDoctor] URGENT: Dr. {} notified about patient {}! Diagnosis: {} (score: {})",
                doctorName, patientId, description, score);
        return "NOTIFIED";
    }
}
