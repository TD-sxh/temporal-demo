package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Activity handler: simulates getting a diagnosis result.
 * Registered name: "getDiagnosisResult"
 *
 * <p>Returns a Map with keys: patientId, code, description, score
 */
public class GetDiagnosisHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetDiagnosisHandler.class);

    @Override
    public Object handle(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        String scenario = (String) input.get("scenario");

        double score;
        if ("normal".equalsIgnoreCase(scenario)) {
            score = 0.2;
        } else if ("abnormal".equalsIgnoreCase(scenario)) {
            score = 0.6;
        } else if ("severe".equalsIgnoreCase(scenario)) {
            score = 0.85;
        } else {
            score = new Random().nextDouble();
        }

        String code;
        String description;
        if (score < 0.4) {
            code = "D-NORMAL-001";
            description = "All vitals within normal range";
        } else if (score < 0.75) {
            code = "D-ABNORMAL-002";
            description = "Elevated blood pressure and cholesterol";
        } else {
            code = "D-SEVERE-003";
            description = "Critical cardiac enzyme levels detected";
        }

        logger.info("[getDiagnosisResult] Patient {} (scenario={}): score={}, code={}",
                patientId, scenario, String.format("%.2f", score), code);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("code", code);
        result.put("description", description);
        result.put("score", score);
        return result;
    }
}
