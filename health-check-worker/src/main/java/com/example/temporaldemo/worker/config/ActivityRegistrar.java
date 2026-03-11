package com.example.temporaldemo.worker.config;

import com.example.temporaldemo.engine.activity.ActivityHandlerRegistry;
import com.example.temporaldemo.engine.model.ActivityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * On startup, pushes all registered activity metadata to the definition-service
 * so workflow designers can discover available activities.
 */
@Component
public class ActivityRegistrar implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRegistrar.class);

    private final ActivityHandlerRegistry registry;
    private final String taskQueue;
    private final String definitionServiceUrl;

    public ActivityRegistrar(
            ActivityHandlerRegistry registry,
            @Value("${engine.task-queue:orchestrator-task-queue}") String taskQueue,
            @Value("${definition.service.url:http://localhost:8082}") String definitionServiceUrl) {
        this.registry = registry;
        this.taskQueue = taskQueue;
        this.definitionServiceUrl = definitionServiceUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ActivityInfo> infos = registry.getAllActivityInfos();
        if (infos.isEmpty()) {
            logger.warn("No activity metadata found — skipping catalog registration.");
            return;
        }

        List<Map<String, Object>> activities = infos.stream()
                .map(info -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("name", info.getName());
                    entry.put("description", info.getDescription());
                    entry.put("inputKeys", info.getInputKeys() != null ? info.getInputKeys() : List.of());
                    entry.put("outputType", info.getOutputType());
                    return entry;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskQueue", taskQueue);
        body.put("activities", activities);

        try {
            RestClient client = RestClient.create();
            Map<?, ?> response = client.post()
                    .uri(definitionServiceUrl + "/api/activities/register")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            logger.info("Activity catalog registered: {} activities → {}", infos.size(), response);
        } catch (Exception e) {
            logger.warn("Failed to register activity catalog with definition-service at {}: {}",
                    definitionServiceUrl, e.getMessage());
        }
    }
}
