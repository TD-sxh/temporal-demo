package com.example.temporaldemo.admin.controller;

import com.example.temporaldemo.admin.service.WorkflowQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowAdminController {

    private final WorkflowQueryService queryService;

    public WorkflowAdminController(WorkflowQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * GET /api/workflows
     * List all workflow executions with optional TQL filter.
     *
     * Query params:
     *   query         — Temporal Query Language filter, e.g. "ExecutionStatus='Running'"
     *   pageSize      — number of results per page (default 20)
     *   nextPageToken — base64-encoded pagination token from previous response
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listWorkflows(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String nextPageToken) {
        return ResponseEntity.ok(queryService.listWorkflows(query, pageSize, nextPageToken));
    }

    /**
     * GET /api/workflows/{workflowId}
     * Describe a single workflow. For RUNNING workflows includes runtime status (variables, paused).
     */
    @GetMapping("/{workflowId}")
    public ResponseEntity<Map<String, Object>> describeWorkflow(
            @PathVariable String workflowId) {
        return ResponseEntity.ok(queryService.describeWorkflow(workflowId));
    }

    /**
     * GET /api/workflows/{workflowId}/events
     * Retrieve the event history of a workflow execution.
     *
     * Query params:
     *   pageSize      — max events per page (default 100)
     *   nextPageToken — base64-encoded pagination token from previous response
     */
    @GetMapping("/{workflowId}/events")
    public ResponseEntity<Map<String, Object>> getEventHistory(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) String nextPageToken) {
        return ResponseEntity.ok(queryService.getEventHistory(workflowId, pageSize, nextPageToken));
    }

    /**
     * POST /api/workflows/{workflowId}/pause
     * Pause a running workflow. The workflow will halt after finishing its current node.
     */
    @PostMapping("/{workflowId}/pause")
    public ResponseEntity<Map<String, Object>> pause(@PathVariable String workflowId) {
        queryService.pause(workflowId);
        return ResponseEntity.ok(Map.of("workflowId", workflowId, "action", "PAUSED"));
    }

    /**
     * POST /api/workflows/{workflowId}/resume
     * Resume a paused workflow.
     */
    @PostMapping("/{workflowId}/resume")
    public ResponseEntity<Map<String, Object>> resume(@PathVariable String workflowId) {
        queryService.resume(workflowId);
        return ResponseEntity.ok(Map.of("workflowId", workflowId, "action", "RESUMED"));
    }

    /**
     * POST /api/workflows/{workflowId}/terminate
     * Forcefully terminate a workflow.
     *
     * Query param:
     *   reason — optional termination reason
     */
    @PostMapping("/{workflowId}/terminate")
    public ResponseEntity<Map<String, Object>> terminate(
            @PathVariable String workflowId,
            @RequestParam(required = false) String reason) {
        queryService.terminate(workflowId, reason);
        return ResponseEntity.ok(Map.of("workflowId", workflowId, "action", "TERMINATED"));
    }

    /**
     * POST /api/workflows/{workflowId}/human-task
     * Send an action signal to the currently waiting HUMAN_TASK node.
     *
     * Request body: { "action": "execute" | "skip" | "terminate" }
     *
     * The service resolves the child workflow ID automatically from the parent's
     * runtime status ({@code pendingHumanTaskId}). Returns 409 if no HUMAN_TASK
     * is currently waiting.
     */
    @PostMapping("/{workflowId}/human-task")
    public ResponseEntity<Map<String, Object>> humanTaskSignal(
            @PathVariable String workflowId,
            @RequestBody Map<String, String> body) {
        String action = body.get("action");
        if (action == null || action.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'action' field is required",
                                 "supported", "execute, skip, terminate"));
        }
        try {
            Map<String, Object> result = queryService.humanTaskSignal(workflowId, action);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
