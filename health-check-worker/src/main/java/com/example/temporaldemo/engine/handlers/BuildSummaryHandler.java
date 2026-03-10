package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: builds a summary of the health check.
 * Registered name: "buildSummary"
 *
 * <p>Returns a summary string.
 */
public class BuildSummaryHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(BuildSummaryHandler.class);

    @Override
    public Object handle(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        String severity = (String) input.get("severity");
        int followUpCount = toInt(input.get("followUpCount"));
        String visitId = input.get("visitId") != null ? input.get("visitId").toString() : "N/A";

        String summary = String.format(
                "Health Check Complete for patient %s (visit: %s). " +
                "Final severity: %s. Total follow-ups: %d.",
                patientId, visitId, severity, followUpCount);

        logger.info("[buildSummary] {}", summary);
        return summary;
    }

    private int toInt(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : 0;
    }
}
