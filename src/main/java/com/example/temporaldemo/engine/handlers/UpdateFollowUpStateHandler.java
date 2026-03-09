package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Activity handler: updates follow-up state (counter, score, severity).
 * Registered name: "updateFollowUpState"
 *
 * <p>Returns a Map with updated followUpCount, currentScore, severity, and maxFollowUps.
 * The orchestrator stores these back into the workflow context so the LOOP
 * condition can reference the updated values.
 */
public class UpdateFollowUpStateHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateFollowUpStateHandler.class);

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(Map<String, Object> input) {
        Object followUpResultObj = input.get("followUpResult");
        int followUpCount = toInt(input.get("followUpCount"));

        int newCount = followUpCount + 1;
        double newScore = 0.0;

        if (followUpResultObj instanceof Map) {
            Map<String, Object> result = (Map<String, Object>) followUpResultObj;
            newScore = toDouble(result.get("healthScore"));
        }

        // Determine severity from score
        String severity;
        int maxFollowUps;
        if (newScore < 0.4) {
            severity = "NORMAL";
            maxFollowUps = 3;
        } else if (newScore < 0.75) {
            severity = "ABNORMAL";
            maxFollowUps = 5;
        } else {
            severity = "SEVERE";
            maxFollowUps = 7;
        }

        logger.info("[updateFollowUpState] count={}, score={}, severity={}, maxFollowUps={}",
                newCount, String.format("%.2f", newScore), severity, maxFollowUps);

        // Return a map; the orchestrator needs a way to "explode" this back
        // into individual context variables. We use a convention:
        // the TASK node's outputKey = "stateUpdate", and we also set
        // individual keys via a special "__setVariables" map.
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("followUpCount", newCount);
        result.put("currentScore", newScore);
        result.put("severity", severity);
        result.put("maxFollowUps", maxFollowUps);
        return result;
    }

    private int toInt(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : 0;
    }

    private double toDouble(Object obj) {
        return obj instanceof Number ? ((Number) obj).doubleValue() : 0.0;
    }
}
