package com.example.temporaldemo.engine.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST endpoints for the activity catalog.
 *
 * <p>Workers call {@code POST /api/activities/register} on startup to publish
 * their available activities. Workflow designers query {@code GET /api/activities}
 * to discover what activities they can reference in TASK nodes.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityCatalogController.class);

    private final ActivityCatalogRepository repository;
    private final ObjectMapper objectMapper;

    public ActivityCatalogController(ActivityCatalogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * List all registered activities.
     *
     * <p>Response example:
     * <pre>
     * [
     *   {
     *     "name": "recordVisit",
     *     "description": "Records a patient visit",
     *     "inputKeys": ["patientId", "patientName"],
     *     "outputType": "string",
     *     "taskQueue": "orchestrator-task-queue",
     *     "registeredAt": "2026-03-10T10:00:00"
     *   }
     * ]
     * </pre>
     */
    @GetMapping
    public List<Map<String, Object>> listActivities() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Register (upsert) a batch of activities from a worker.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "taskQueue": "orchestrator-task-queue",
     *   "activities": [
     *     {
     *       "name": "recordVisit",
     *       "description": "Records a patient visit",
     *       "inputKeys": ["patientId", "patientName", "doctorName", "visitReason"],
     *       "outputType": "string"
     *     }
     *   ]
     * }
     * </pre>
     */
    @PostMapping("/register")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        String taskQueue = (String) request.get("taskQueue");
        List<Map<String, Object>> activities = (List<Map<String, Object>>) request.get("activities");

        if (activities == null || activities.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "activities list is required"));
        }

        int registered = 0;
        for (Map<String, Object> activity : activities) {
            String name = (String) activity.get("name");
            if (name == null || name.isBlank()) continue;

            ActivityCatalogEntity entity = repository.findByName(name)
                    .orElseGet(ActivityCatalogEntity::new);
            entity.setName(name);
            entity.setDescription((String) activity.get("description"));
            entity.setOutputType((String) activity.get("outputType"));
            entity.setTaskQueue(taskQueue);
            entity.setRegisteredAt(LocalDateTime.now());

            // Serialize inputKeys list to JSON string
            List<String> inputKeys = (List<String>) activity.get("inputKeys");
            if (inputKeys != null) {
                try {
                    entity.setInputKeysJson(objectMapper.writeValueAsString(inputKeys));
                } catch (Exception e) {
                    entity.setInputKeysJson("[]");
                }
            }

            repository.save(entity);
            registered++;
        }

        logger.info("Activity catalog updated: {} activities registered from task queue '{}'", registered, taskQueue);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("registered", registered);
        response.put("taskQueue", taskQueue);
        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toResponse(ActivityCatalogEntity entity) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("name", entity.getName());
        resp.put("description", entity.getDescription());
        resp.put("outputType", entity.getOutputType());
        resp.put("taskQueue", entity.getTaskQueue());
        resp.put("registeredAt", entity.getRegisteredAt());

        // Parse inputKeysJson back to a List
        List<String> inputKeys = List.of();
        if (entity.getInputKeysJson() != null && !entity.getInputKeysJson().isBlank()) {
            try {
                inputKeys = objectMapper.readValue(entity.getInputKeysJson(), List.class);
            } catch (Exception ignored) {}
        }
        resp.put("inputKeys", inputKeys);
        return resp;
    }
}
