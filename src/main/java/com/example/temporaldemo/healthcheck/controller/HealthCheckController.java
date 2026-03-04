package com.example.temporaldemo.healthcheck.controller;

import com.example.temporaldemo.healthcheck.HealthCheckConstants;
import com.example.temporaldemo.healthcheck.model.*;
import com.example.temporaldemo.healthcheck.workflow.HealthCheckWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * REST controller for health check workflow operations.
 *
 * Replaces the CLI-based SignalSender and HealthCheckStarter with HTTP endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST   /api/healthcheck/start                     — Start a new workflow</li>
 *   <li>GET    /api/healthcheck/{workflowId}/status        — Query workflow status</li>
 *   <li>POST   /api/healthcheck/{workflowId}/cancel        — Signal: cancel follow-ups</li>
 *   <li>POST   /api/healthcheck/{workflowId}/labresult     — Signal: inject new lab result</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/healthcheck")
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

    private final WorkflowClient workflowClient;

    public HealthCheckController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * Start a new health check workflow.
     *
     * <p>Example:
     * <pre>
     * curl -X POST http://localhost:8081/api/healthcheck/start \
     *   -H "Content-Type: application/json" \
     *   -d '{"patientId":"P001","patientName":"John Doe","doctorName":"Dr. Smith","visitReason":"Annual physical exam","scenario":"severe"}'
     * </pre>
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startWorkflow(@RequestBody StartHealthCheckRequest request) {
        String patientId = request.getPatientId();
        String workflowId = "health-check-" + patientId;

        HealthCheckWorkflow workflow = workflowClient.newWorkflowStub(
                HealthCheckWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(HealthCheckConstants.TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                        .build());

        PatientVisit visit = new PatientVisit(
                patientId,
                request.getPatientName(),
                request.getDoctorName(),
                request.getVisitReason(),
                request.getScenario(),
                request.isDemoMode());

        WorkflowClient.start(workflow::processHealthCheck, visit);

        logger.info("Workflow started: {} (scenario={})", workflowId, request.getScenario());

        return ResponseEntity.ok(Map.of(
                "workflowId", workflowId,
                "patientId", patientId,
                "message", "Workflow started successfully"
        ));
    }

    /**
     * Query workflow status.
     *
     * <p>Example:
     * <pre>
     * curl http://localhost:8081/api/healthcheck/health-check-P001/status
     * </pre>
     */
    @GetMapping("/{workflowId}/status")
    public ResponseEntity<?> getStatus(@PathVariable String workflowId) {
        try {
            HealthCheckWorkflow workflow = workflowClient.newWorkflowStub(HealthCheckWorkflow.class, workflowId);
            HealthCheckStatus status = workflow.getStatus();
            logger.info("Query status for {}: {}", workflowId, status);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.warn("Failed to query workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to query workflow",
                    "workflowId", workflowId,
                    "detail", e.getMessage()
            ));
        }
    }

    /**
     * Signal: cancel follow-ups.
     *
     * <p>Example:
     * <pre>
     * curl -X POST http://localhost:8081/api/healthcheck/health-check-P001/cancel \
     *   -H "Content-Type: application/json" \
     *   -d '{"reason":"Patient transferred to another hospital"}'
     * </pre>
     */
    @PostMapping("/{workflowId}/cancel")
    public ResponseEntity<Map<String, String>> cancelFollowUp(
            @PathVariable String workflowId,
            @RequestBody(required = false) CancelRequest request) {

        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "Cancelled by operator via HTTP";

        try {
            HealthCheckWorkflow workflow = workflowClient.newWorkflowStub(HealthCheckWorkflow.class, workflowId);
            workflow.cancelFollowUp(reason);
            logger.info("Signal 'cancelFollowUp' sent to {}. Reason: {}", workflowId, reason);

            return ResponseEntity.ok(Map.of(
                    "workflowId", workflowId,
                    "signal", "cancelFollowUp",
                    "reason", reason,
                    "message", "Cancel signal sent successfully"
            ));
        } catch (Exception e) {
            logger.warn("Failed to send cancel signal to {}: {}", workflowId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send cancel signal",
                    "workflowId", workflowId,
                    "detail", e.getMessage()
            ));
        }
    }

    /**
     * Signal: inject a new lab result score.
     *
     * <p>Example:
     * <pre>
     * curl -X POST http://localhost:8081/api/healthcheck/health-check-P001/labresult \
     *   -H "Content-Type: application/json" \
     *   -d '{"score":0.9}'
     * </pre>
     */
    @PostMapping("/{workflowId}/labresult")
    public ResponseEntity<Map<String, String>> newLabResult(
            @PathVariable String workflowId,
            @RequestBody LabResultRequest request) {

        double score = request.getScore();
        if (score < 0.0 || score > 1.0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Score must be between 0.0 and 1.0",
                    "score", String.valueOf(score)
            ));
        }

        try {
            HealthCheckWorkflow workflow = workflowClient.newWorkflowStub(HealthCheckWorkflow.class, workflowId);
            workflow.newLabResult(score);
            logger.info("Signal 'newLabResult' sent to {}. Score: {}", workflowId, score);

            return ResponseEntity.ok(Map.of(
                    "workflowId", workflowId,
                    "signal", "newLabResult",
                    "score", String.format("%.2f", score),
                    "message", "Lab result signal sent successfully"
            ));
        } catch (Exception e) {
            logger.warn("Failed to send labresult signal to {}: {}", workflowId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send lab result signal",
                    "workflowId", workflowId,
                    "detail", e.getMessage()
            ));
        }
    }
}
