package com.example.temporaldemo.engine.controller;

import com.example.temporaldemo.engine.definition.WorkflowDefinitionService;
import com.example.temporaldemo.engine.definition.WorkflowDefinitionService.ResolvedDefinition;
import com.example.temporaldemo.engine.model.InputParam;
import com.example.temporaldemo.engine.model.NodeDefinition;
import com.example.temporaldemo.engine.model.NodeType;
import com.example.temporaldemo.engine.model.WorkflowDefinition;
import com.example.temporaldemo.engine.workflow.OrchestratorWorkflow;
import tools.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

/**
 * REST controller for the generic workflow orchestration engine.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST   /api/engine/start           — Start a workflow by type</li>
 *   <li>POST   /api/engine/batch-start     — Batch-start multiple workflows</li>
 *   <li>GET    /api/engine/{id}/status      — Query workflow status</li>
 *   <li>POST   /api/engine/{id}/signal      — Send a signal to a running workflow</li>
 *   <li>GET    /api/engine/schema/{type}    — Get input schema for a flow type</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/engine")
public class EngineController {

    private static final Logger logger = LoggerFactory.getLogger(EngineController.class);

    @Value("${engine.task-queue:orchestrator-task-queue}")
    private String taskQueue;

    private final WorkflowClient workflowClient;
    private final WorkflowDefinitionService definitionService;
    private final ObjectMapper objectMapper;

    public EngineController(WorkflowClient workflowClient,
                            WorkflowDefinitionService definitionService,
                            ObjectMapper objectMapper) {
        this.workflowClient = workflowClient;
        this.definitionService = definitionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Start a new workflow by type.
     *
     * <p>The definition is resolved from DB (latest PUBLISHED version) with
     * classpath fallback. The resolved version is recorded as a snapshot
     * in the workflow's input variables ({@code _definitionVersion},
     * {@code _definitionSource}).
     *
     * <p>Request body:
     * <pre>
     * {
     *   "type": "health-check-flow",
     *   "workflowId": "hc-001",          // optional, auto-generated if omitted
     *   "version": 2,                     // optional, default = latest PUBLISHED
     *   "inputVariables": { "patientId": "P001", ... }
     * }
     * </pre>
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startWorkflow(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        String workflowId = (String) request.get("workflowId");
        Integer version = request.get("version") != null
                ? ((Number) request.get("version")).intValue() : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> inputVars = (Map<String, Object>) request.get("inputVariables");

        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "type is required"));
        }

        // Resolve definition (DB → classpath fallback)
        ResolvedDefinition resolved = (version != null)
                ? definitionService.resolve(type, version)
                : definitionService.resolve(type);

        // Validate required inputs against START node's inputSchema
        List<String> validationErrors = validateInputs(resolved.definitionJson(), inputVars);
        if (!validationErrors.isEmpty()) {
            Map<String, Object> errorResp = new LinkedHashMap<>();
            errorResp.put("error", "Missing required input parameters");
            errorResp.put("missingParams", validationErrors);
            return ResponseEntity.badRequest().body(errorResp);
        }

        // Apply defaults from inputSchema for optional params not provided
        inputVars = applyDefaults(resolved.definitionJson(), inputVars);

        // Auto-generate workflowId if not provided
        if (workflowId == null || workflowId.isBlank()) {
            workflowId = type + "-" + System.currentTimeMillis();
        }

        // Inject snapshot metadata into input variables
        Map<String, Object> enrichedVars = new LinkedHashMap<>();
        if (inputVars != null) {
            enrichedVars.putAll(inputVars);
        }
        enrichedVars.put("_definitionType", type);
        enrichedVars.put("_definitionVersion", resolved.version());
        enrichedVars.put("_definitionSource", resolved.source());

        // Start the workflow
        OrchestratorWorkflow workflow = workflowClient.newWorkflowStub(
                OrchestratorWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(taskQueue)
                        .setWorkflowId(workflowId)
                        .setWorkflowExecutionTimeout(Duration.ofHours(24))
                        .build());

        WorkflowClient.start(workflow::execute, resolved.definitionJson(), enrichedVars);
        logger.info("Engine workflow started: {} (type={}, version={}, source={})",
                workflowId, type, resolved.version(), resolved.source());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("workflowId", workflowId);
        response.put("type", type);
        response.put("version", resolved.version());
        response.put("source", resolved.source());
        response.put("message", "Workflow started successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Batch-start multiple workflows of the same type.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "type": "health-check-flow",
     *   "version": 2,                        // optional
     *   "items": [
     *     { "workflowId": "hc-001", "inputVariables": { ... } },
     *     { "workflowId": "hc-002", "inputVariables": { ... } }
     *   ]
     * }
     * </pre>
     *
     * <p>Each item is started independently; partial failures do not block others.
     */
    @PostMapping("/batch-start")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> batchStart(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        Integer version = request.get("version") != null
                ? ((Number) request.get("version")).intValue() : null;
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "type is required"));
        }
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "items array is required and must not be empty"));
        }

        // Resolve definition once for all items
        ResolvedDefinition resolved = (version != null)
                ? definitionService.resolve(type, version)
                : definitionService.resolve(type);

        List<Map<String, Object>> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String workflowId = (String) item.get("workflowId");
            Map<String, Object> inputVars = (Map<String, Object>) item.get("inputVariables");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("index", i);

            try {
                // Validate inputs
                List<String> validationErrors = validateInputs(resolved.definitionJson(), inputVars);
                if (!validationErrors.isEmpty()) {
                    result.put("status", "FAILED");
                    result.put("error", "Missing required params: " + validationErrors);
                    failed++;
                    results.add(result);
                    continue;
                }

                inputVars = applyDefaults(resolved.definitionJson(), inputVars);

                if (workflowId == null || workflowId.isBlank()) {
                    workflowId = type + "-" + System.currentTimeMillis() + "-" + i;
                }

                Map<String, Object> enrichedVars = new LinkedHashMap<>();
                if (inputVars != null) enrichedVars.putAll(inputVars);
                enrichedVars.put("_definitionType", type);
                enrichedVars.put("_definitionVersion", resolved.version());
                enrichedVars.put("_definitionSource", resolved.source());

                OrchestratorWorkflow workflow = workflowClient.newWorkflowStub(
                        OrchestratorWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(taskQueue)
                                .setWorkflowId(workflowId)
                                .setWorkflowExecutionTimeout(Duration.ofHours(24))
                                .build());

                WorkflowClient.start(workflow::execute, resolved.definitionJson(), enrichedVars);

                result.put("workflowId", workflowId);
                result.put("status", "STARTED");
                succeeded++;
            } catch (Exception e) {
                result.put("workflowId", workflowId);
                result.put("status", "FAILED");
                result.put("error", e.getMessage());
                failed++;
            }
            results.add(result);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", type);
        response.put("version", resolved.version());
        response.put("source", resolved.source());
        response.put("total", items.size());
        response.put("succeeded", succeeded);
        response.put("failed", failed);
        response.put("results", results);

        HttpStatus httpStatus = failed == items.size() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Get the input schema for a workflow type.
     * Reads the START node's inputSchema from the definition.
     */
    @GetMapping("/schema/{type}")
    public ResponseEntity<Map<String, Object>> getSchema(@PathVariable String type) {
        try {
            ResolvedDefinition resolved = definitionService.resolve(type);
            Map<String, InputParam> schema = extractInputSchema(resolved.definitionJson());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", type);
            response.put("version", resolved.version());
            response.put("source", resolved.source());
            response.put("inputSchema", schema != null ? schema : Map.of());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Query the status of a running workflow.
     *
     * <p>Example:
     * <pre>
     * curl http://localhost:8081/api/engine/my-workflow-001/status
     * </pre>
     */
    @GetMapping("/{workflowId}/status")
    public Map<String, Object> getStatus(@PathVariable String workflowId) {
        try {
            OrchestratorWorkflow workflow = workflowClient.newWorkflowStub(
                    OrchestratorWorkflow.class, workflowId);
            return workflow.getStatus();
        } catch (Exception e) {
            logger.warn("Failed to query workflow {}: {}", workflowId, e.getMessage());
            return Map.of(
                    "error", "Failed to query workflow",
                    "workflowId", workflowId,
                    "detail", e.getMessage()
            );
        }
    }

    /**
     * Send a signal to a running workflow.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "signalName": "labResult",
     *   "payload": 0.85
     * }
     * </pre>
     */
    @PostMapping("/{workflowId}/signal")
    public Map<String, String> sendSignal(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> request) {
        String signalName = (String) request.get("signalName");
        Object payload = request.get("payload");

        try {
            OrchestratorWorkflow workflow = workflowClient.newWorkflowStub(
                    OrchestratorWorkflow.class, workflowId);
            workflow.signal(signalName, payload);
            logger.info("Signal '{}' sent to workflow {} with payload: {}", signalName, workflowId, payload);
            return Map.of(
                    "workflowId", workflowId,
                    "signalName", signalName,
                    "message", "Signal sent successfully"
            );
        } catch (Exception e) {
            logger.warn("Failed to signal workflow {}: {}", workflowId, e.getMessage());
            return Map.of(
                    "error", "Failed to send signal",
                    "workflowId", workflowId,
                    "detail", e.getMessage()
            );
        }
    }

    // ─── Private helpers ────────────────────────────────

    /**
     * Extract the inputSchema from the START node, or null if not defined.
     */
    private Map<String, InputParam> extractInputSchema(String definitionJson) {
        try {
            WorkflowDefinition def = objectMapper.readValue(definitionJson, WorkflowDefinition.class);
            if (def.getNodes() == null) return null;
            for (NodeDefinition node : def.getNodes()) {
                if (node.getType() == NodeType.START && node.getInputSchema() != null) {
                    return node.getInputSchema();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse definition for schema extraction: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Validate inputVariables against the START node's inputSchema.
     * Returns a list of missing required parameter names (empty = valid).
     */
    private List<String> validateInputs(String definitionJson, Map<String, Object> inputVars) {
        Map<String, InputParam> schema = extractInputSchema(definitionJson);
        if (schema == null || schema.isEmpty()) {
            return List.of(); // No schema → no validation
        }

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, InputParam> entry : schema.entrySet()) {
            String paramName = entry.getKey();
            InputParam param = entry.getValue();
            if (param.isRequired()) {
                if (inputVars == null || !inputVars.containsKey(paramName)
                        || inputVars.get(paramName) == null) {
                    missing.add(paramName);
                }
            }
        }
        return missing;
    }

    /**
     * Apply default values from the input schema for optional params not provided.
     */
    private Map<String, Object> applyDefaults(String definitionJson, Map<String, Object> inputVars) {
        Map<String, InputParam> schema = extractInputSchema(definitionJson);
        if (schema == null || schema.isEmpty()) {
            return inputVars;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (inputVars != null) result.putAll(inputVars);

        for (Map.Entry<String, InputParam> entry : schema.entrySet()) {
            String paramName = entry.getKey();
            InputParam param = entry.getValue();
            if (!result.containsKey(paramName) && param.getDefaultValue() != null) {
                result.put(paramName, param.getDefaultValue());
            }
        }
        return result;
    }
}
