package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: analyzes diagnosis results and determines severity.
 * Registered name: "analyzeResults"
 *
 * <p>Returns the severity string: "NORMAL", "ABNORMAL", or "SEVERE"
 */
public class AnalyzeResultsHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeResultsHandler.class);

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(Map<String, Object> input) {
        Object diagnosisObj = input.get("diagnosis");
        double score;
        if (diagnosisObj instanceof Map) {
            Map<String, Object> diagnosis = (Map<String, Object>) diagnosisObj;
            score = ((Number) diagnosis.get("score")).doubleValue();
        } else {
            score = 0.5; // fallback
        }

        String severity;
        if (score < 0.4) {
            severity = "NORMAL";
        } else if (score < 0.75) {
            severity = "ABNORMAL";
        } else {
            severity = "SEVERE";
        }

        logger.info("[analyzeResults] score={} -> {}", String.format("%.2f", score), severity);
        return severity;
    }
}
