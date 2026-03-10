package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Activity handler: performs a follow-up and returns the result.
 * Registered name: "performFollowUp"
 *
 * <p>Returns a Map with keys: patientId, sequenceNumber, notes, timestamp, healthScore
 */
public class PerformFollowUpHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(PerformFollowUpHandler.class);

    @Override
    public Object handle(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        int followUpCount = toInt(input.get("followUpCount"));
        int sequenceNumber = followUpCount + 1;
        String severity = (String) input.get("severity");
        double previousScore = toDouble(input.get("currentScore"));

        // Simulate health score change: patient generally improves
        double delta = (new Random().nextDouble() - 0.3) * 0.2;
        double newScore = Math.max(0.0, Math.min(1.0, previousScore + delta));

        String trend;
        if (newScore < previousScore - 0.05) {
            trend = "IMPROVING";
        } else if (newScore > previousScore + 0.05) {
            trend = "WORSENING";
        } else {
            trend = "STABLE";
        }

        String notes = String.format("Follow-up #%d for %s case. Previous: %.2f -> New: %.2f (%s)",
                sequenceNumber, severity, previousScore, newScore, trend);
        String timestamp = Instant.now().toString();

        logger.info("[performFollowUp] Patient {}: #{} | {} -> {} ({}) at {}",
                patientId, sequenceNumber, String.format("%.2f", previousScore),
                String.format("%.2f", newScore), trend, timestamp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("sequenceNumber", sequenceNumber);
        result.put("notes", notes);
        result.put("timestamp", timestamp);
        result.put("healthScore", newScore);
        return result;
    }

    private int toInt(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : 0;
    }

    private double toDouble(Object obj) {
        return obj instanceof Number ? ((Number) obj).doubleValue() : 0.0;
    }
}
