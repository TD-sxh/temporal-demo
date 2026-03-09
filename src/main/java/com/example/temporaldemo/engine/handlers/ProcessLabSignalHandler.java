package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: processes a lab signal value.
 * Registered name: "processLabSignal"
 *
 * <p>If labSignal is present, returns the new score; otherwise returns currentScore unchanged.
 */
public class ProcessLabSignalHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProcessLabSignalHandler.class);

    @Override
    public Object handle(Map<String, Object> input) {
        Object labSignal = input.get("labSignal");
        Object currentScoreObj = input.get("currentScore");
        double currentScore = currentScoreObj instanceof Number ? ((Number) currentScoreObj).doubleValue() : 0.0;

        if (labSignal instanceof Number) {
            double newScore = ((Number) labSignal).doubleValue();
            logger.info("[processLabSignal] Lab signal applied: {} -> {}", 
                    String.format("%.2f", currentScore), String.format("%.2f", newScore));
            return newScore;
        }

        logger.info("[processLabSignal] No lab signal, keeping current score: {}", String.format("%.2f", currentScore));
        return currentScore;
    }
}
