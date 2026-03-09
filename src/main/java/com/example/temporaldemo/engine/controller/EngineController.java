package com.example.temporaldemo.engine.controller;

import com.example.temporaldemo.engine.definition.WorkflowDefinitionService;
import com.example.temporaldemo.engine.definition.WorkflowDefinitionService.ResolvedDefinition;
import com.example.temporaldemo.engine.workflow.OrchestratorWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for the generic workflow orchestration engine.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST   /api/engine/start           — Start a workflow by type (resolves from DB → classpath)</li>
 *   <li>GET    /api/engine/{id}/status      — Query workflow status</li>
 *   <li>POST   /api/engine/{id}/signal      — Send a signal to a running workflow</li>
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

    public EngineController(WorkflowClient workflowClient,
                            WorkflowDefinitionService definitionService) {
        this.workflowClient = workflowClient;
        this.definitionService = definitionService;
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
    public Map<String, Object> startWorkflow(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        String workflowId = (String) request.get("workflowId");
        Integer version = request.get("version") != null
                ? ((Number) request.get("version")).intValue() : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> inputVars = (Map<String, Object>) request.get("inputVariables");

        if (type == null || type.isBlank()) {
            return Map.of("error", "type is required");
        }

        // Resolve definition (DB → classpath fallback)
        ResolvedDefinition resolved = (version != null)
                ? definitionService.resolve(type, version)
                : definitionService.resolve(type);

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
        return response;
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
}
