package com.example.temporaldemo.worker.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo implementation of {@link NotificationActivities}.
 *
 * <p>All methods log the notification details and return a status map.
 * No real email / SMS / phone provider is called.
 */
public class NotificationActivitiesImpl implements NotificationActivities {

    private static final Logger logger = LoggerFactory.getLogger(NotificationActivitiesImpl.class);

    @Override
    public Object sendNotificationEmail(Map<String, Object> input) {
        logger.info("[DEMO EMAIL] ─────────────────────────────────────────");
        logger.info("[DEMO EMAIL]  To      : {}", input.get("to"));
        logger.info("[DEMO EMAIL]  Subject : Human Task '{}' requires your action", input.get("taskName"));
        logger.info("[DEMO EMAIL]  Body    : {}", input.get("message"));
        logger.info("[DEMO EMAIL]  Workflow: {}", input.get("workflowId"));
        logger.info("[DEMO EMAIL] ─────────────────────────────────────────");
        return buildResult("EMAIL", input);
    }

    @Override
    public Object sendNotificationSms(Map<String, Object> input) {
        logger.info("[DEMO SMS] ─────────────────────────────────────────");
        logger.info("[DEMO SMS]  To     : {}", input.get("phone"));
        logger.info("[DEMO SMS]  Message: [Task: {}] {} (wf: {})",
                input.get("taskName"), input.get("message"), input.get("workflowId"));
        logger.info("[DEMO SMS] ─────────────────────────────────────────");
        return buildResult("SMS", input);
    }

    @Override
    public Object sendNotificationCall(Map<String, Object> input) {
        logger.info("[DEMO CALL] ─────────────────────────────────────────");
        logger.info("[DEMO CALL]  Calling : {}", input.get("phone"));
        logger.info("[DEMO CALL]  Script  : Hello, this is an automated call regarding task '{}'. {}", 
                input.get("taskName"), input.get("message"));
        logger.info("[DEMO CALL]  Workflow: {}", input.get("workflowId"));
        logger.info("[DEMO CALL] ─────────────────────────────────────────");
        return buildResult("PHONE", input);
    }

    private static Map<String, Object> buildResult(String channel, Map<String, Object> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", channel);
        result.put("status", "SENT");
        result.put("taskId", input.get("taskId"));
        result.put("sentAt", Instant.now().toString());
        return result;
    }
}
