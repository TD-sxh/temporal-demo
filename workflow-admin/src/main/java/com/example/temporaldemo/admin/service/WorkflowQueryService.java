package com.example.temporaldemo.admin.service;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Memo;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class WorkflowQueryService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowQueryService.class);

    private final WorkflowServiceStubs serviceStubs;
    private final WorkflowClient workflowClient;
    private final String namespace;

    public WorkflowQueryService(
            WorkflowServiceStubs serviceStubs,
            WorkflowClient workflowClient,
            @Value("${temporal.namespace:default}") String namespace) {
        this.serviceStubs = serviceStubs;
        this.workflowClient = workflowClient;
        this.namespace = namespace;
    }

    // ─── List ────────────────────────────────────────

    /**
     * List workflow executions. Supports Temporal Query Language (TQL) filter.
     * Examples:
     *   query = "WorkflowType='OrchestratorWorkflow'"
     *   query = "ExecutionStatus='Running'"
     *   query = "WorkflowId='hc-patient-001'"
     */
    public Map<String, Object> listWorkflows(String query, int pageSize, String nextPageTokenBase64) {
        ListWorkflowExecutionsRequest.Builder builder = ListWorkflowExecutionsRequest.newBuilder()
                .setNamespace(namespace)
                .setPageSize(pageSize > 0 ? pageSize : 20);

        if (query != null && !query.isBlank()) {
            builder.setQuery(query);
        }
        if (nextPageTokenBase64 != null && !nextPageTokenBase64.isBlank()) {
            builder.setNextPageToken(
                    ByteString.copyFrom(Base64.getDecoder().decode(nextPageTokenBase64)));
        }

        ListWorkflowExecutionsResponse response =
                serviceStubs.blockingStub().listWorkflowExecutions(builder.build());

        // Exclude child workflows (HumanTask child flows) — they are nested under their parent
        List<Map<String, Object>> workflows = response.getExecutionsList().stream()
                .filter(info -> !isChildWorkflow(info.getExecution().getWorkflowId()))
                .map(info -> {
                    Map<String, Object> item = toInfoMap(info);
                    // Enrich RUNNING workflows with live paused state via getStatus() query.
                    // (Postgres standard-visibility doesn't include SearchAttributes in list response)
                    if ("RUNNING".equals(item.get("status"))) {
                        try {
                            WorkflowStub stub = workflowClient.newUntypedWorkflowStub(
                                    info.getExecution().getWorkflowId(), Optional.empty(), Optional.empty());
                            Map<?, ?> runtimeStatus = stub.query("getStatus", Map.class);
                            Boolean paused = (Boolean) runtimeStatus.get("paused");
                            if (Boolean.TRUE.equals(paused)) {
                                item.put("status", "PAUSED");
                            }
                        } catch (Exception e) {
                            logger.info("Could not query paused state for {}: {}", info.getExecution().getWorkflowId(), e.getMessage());
                        }
                    }
                    return item;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", workflows.size());
        result.put("workflows", workflows);

        ByteString nextToken = response.getNextPageToken();
        if (!nextToken.isEmpty()) {
            result.put("nextPageToken",
                    Base64.getEncoder().encodeToString(nextToken.toByteArray()));
        }

        return result;
    }

    // ─── Describe ────────────────────────────────────

    /**
     * Describe a workflow. If RUNNING, also calls the 'getStatus' query
     * to fetch runtime variables and paused state.
     */
    public Map<String, Object> describeWorkflow(String workflowId) {
        DescribeWorkflowExecutionResponse response = serviceStubs.blockingStub()
                .describeWorkflowExecution(DescribeWorkflowExecutionRequest.newBuilder()
                        .setNamespace(namespace)
                        .setExecution(WorkflowExecution.newBuilder()
                                .setWorkflowId(workflowId).build())
                        .build());

        WorkflowExecutionInfo info = response.getWorkflowExecutionInfo();
        Map<String, Object> detail = toInfoMap(info);
        detail.put("historyLength", info.getHistoryLength());
        detail.put("pendingActivities", response.getPendingActivitiesCount());

        // For running workflows, fetch live runtime status via query
        if (info.getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
            try {
                WorkflowStub stub = workflowClient.newUntypedWorkflowStub(
                        workflowId, Optional.empty(), Optional.empty());
                Map<?, ?> runtimeStatus = stub.query("getStatus", Map.class);
                detail.put("runtimeStatus", runtimeStatus);

                // Reflect PAUSED state in top-level status (same as list endpoint)
                if (Boolean.TRUE.equals(runtimeStatus.get("paused"))) {
                    detail.put("status", "PAUSED");
                }

                // If a DIGITAL_MESSAGE child workflow is pending, embed its detail
                Object childId = runtimeStatus.get("pendingDigitalMessageId");
                if (childId != null) {
                    try {
                        detail.put("pendingDigitalMessage", describeChildWorkflow(childId.toString()));
                    } catch (Exception ex) {
                        logger.debug("Could not describe child workflow {}: {}", childId, ex.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not query runtime status for {}: {}", workflowId, e.getMessage());
            }
        }

        return detail;
    }

    /**
     * Describe a child (HumanTask) workflow — lighter version without recursive child lookup.
     */
    private Map<String, Object> describeChildWorkflow(String childWorkflowId) {
        DescribeWorkflowExecutionResponse resp = serviceStubs.blockingStub()
                .describeWorkflowExecution(DescribeWorkflowExecutionRequest.newBuilder()
                        .setNamespace(namespace)
                        .setExecution(WorkflowExecution.newBuilder()
                                .setWorkflowId(childWorkflowId).build())
                        .build());
        WorkflowExecutionInfo childInfo = resp.getWorkflowExecutionInfo();
        Map<String, Object> child = toInfoMap(childInfo);
        child.put("historyLength", childInfo.getHistoryLength());
        return child;
    }

    /** Child workflows follow the pattern: parent__digitalMessage__nodeId */
    private static boolean isChildWorkflow(String workflowId) {
        return workflowId != null && workflowId.contains("__digitalMessage__");
    }

    // ─── Event History ────────────────────────────────

    /**
     * Retrieve event history for a workflow execution.
     *
     * @param workflowId          the workflow ID
     * @param pageSize            max events per page (default 100)
     * @param nextPageTokenBase64 base64-encoded pagination token from previous response
     */
    public Map<String, Object> getEventHistory(String workflowId, int pageSize, String nextPageTokenBase64) {
        GetWorkflowExecutionHistoryRequest.Builder builder = GetWorkflowExecutionHistoryRequest.newBuilder()
                .setNamespace(namespace)
                .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                .setMaximumPageSize(pageSize > 0 ? pageSize : 100);

        if (nextPageTokenBase64 != null && !nextPageTokenBase64.isBlank()) {
            builder.setNextPageToken(
                    ByteString.copyFrom(Base64.getDecoder().decode(nextPageTokenBase64)));
        }

        GetWorkflowExecutionHistoryResponse response =
                serviceStubs.blockingStub().getWorkflowExecutionHistory(builder.build());

        List<Map<String, Object>> events = response.getHistory().getEventsList().stream()
                .map(this::toEventMap)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflowId", workflowId);
        result.put("total", events.size());
        result.put("events", events);

        ByteString nextToken = response.getNextPageToken();
        if (!nextToken.isEmpty()) {
            result.put("nextPageToken",
                    Base64.getEncoder().encodeToString(nextToken.toByteArray()));
        }

        return result;
    }

    // ─── Control ─────────────────────────────────────

    public void pause(String workflowId) {
        workflowClient.newUntypedWorkflowStub(workflowId, Optional.empty(), Optional.empty())
                .signal("pause");
        logger.info("Sent pause signal to workflow: {}", workflowId);
    }

    public void resume(String workflowId) {
        workflowClient.newUntypedWorkflowStub(workflowId, Optional.empty(), Optional.empty())
                .signal("resume");
        logger.info("Sent resume signal to workflow: {}", workflowId);
    }

    public void terminate(String workflowId, String reason) {
        workflowClient.newUntypedWorkflowStub(workflowId, Optional.empty(), Optional.empty())
                .terminate(reason != null ? reason : "Terminated via workflow-admin");
        logger.info("Terminated workflow: {} (reason: {})", workflowId, reason);
    }

    /**
     * Send an action signal to the currently waiting DIGITAL_MESSAGE child workflow.
     *
     * @param parentWorkflowId the parent (orchestrator) workflow ID
     * @param action           one of: execute, skip, terminate, cancel
     */
    public Map<String, Object> digitalMessageSignal(String parentWorkflowId, String action) {
        // Resolve child workflow ID from parent status
        String childWorkflowId = resolvePendingDigitalMessageId(parentWorkflowId);

        workflowClient.newUntypedWorkflowStub(childWorkflowId, Optional.empty(), Optional.empty())
                .signal("digitalMessageSignal", action);
        logger.info("Sent digitalMessage action '{}' to child workflow: {}", action, childWorkflowId);

        return Map.of(
                "parentWorkflowId", parentWorkflowId,
                "childWorkflowId", childWorkflowId,
                "action", action.toUpperCase());
    }

    /**
     * Queries the parent workflow for the {@code pendingDigitalMessageId} field.
     * Throws if no DIGITAL_MESSAGE node is currently waiting.
     */
    private String resolvePendingDigitalMessageId(String parentWorkflowId) {
        try {
            WorkflowStub stub = workflowClient.newUntypedWorkflowStub(
                    parentWorkflowId, Optional.empty(), Optional.empty());
            Map<?, ?> status = stub.query("getStatus", Map.class);
            Object childId = status.get("pendingDigitalMessageId");
            if (childId == null) {
                throw new IllegalStateException(
                        "Workflow '" + parentWorkflowId + "' has no pending DIGITAL_MESSAGE at this time.");
            }
            return childId.toString();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to resolve pendingDigitalMessageId for workflow '" + parentWorkflowId + "': " + e.getMessage(), e);
        }
    }

    // ─── Helpers ─────────────────────────────────────

    private Map<String, Object> toEventMap(HistoryEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", event.getEventId());
        if (event.hasEventTime()) {
            map.put("eventTime", Instant.ofEpochSecond(
                    event.getEventTime().getSeconds(),
                    event.getEventTime().getNanos()).toString());
        }
        // Strip EVENT_TYPE_ prefix for readability
        String type = event.getEventType().name().replace("EVENT_TYPE_", "");
        map.put("eventType", type);

        Map<String, Object> attrs = extractEventAttributes(event);
        if (!attrs.isEmpty()) {
            map.put("attributes", attrs);
        }
        return map;
    }

    @SuppressWarnings("deprecation")
    private Map<String, Object> extractEventAttributes(HistoryEvent event) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        switch (event.getAttributesCase()) {
            case WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES -> {
                var a = event.getWorkflowExecutionStartedEventAttributes();
                attrs.put("workflowType", a.getWorkflowType().getName());
                attrs.put("taskQueue", a.getTaskQueue().getName());
            }
            case WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES -> {
                attrs.put("result", "completed");
            }
            case WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES -> {
                var a = event.getWorkflowExecutionFailedEventAttributes();
                if (a.hasFailure()) attrs.put("message", a.getFailure().getMessage());
            }
            case WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES -> {
                var a = event.getWorkflowExecutionTerminatedEventAttributes();
                attrs.put("reason", a.getReason());
            }
            case WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES -> {
                attrs.put("retryState", event.getWorkflowExecutionTimedOutEventAttributes().getRetryState().name());
            }
            case WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES -> {
                var a = event.getWorkflowExecutionSignaledEventAttributes();
                attrs.put("signalName", a.getSignalName());
            }
            case ACTIVITY_TASK_SCHEDULED_EVENT_ATTRIBUTES -> {
                var a = event.getActivityTaskScheduledEventAttributes();
                attrs.put("activityType", a.getActivityType().getName());
                attrs.put("activityId", a.getActivityId());
                attrs.put("taskQueue", a.getTaskQueue().getName());
            }
            case ACTIVITY_TASK_STARTED_EVENT_ATTRIBUTES -> {
                attrs.put("scheduledEventId", event.getActivityTaskStartedEventAttributes().getScheduledEventId());
            }
            case ACTIVITY_TASK_COMPLETED_EVENT_ATTRIBUTES -> {
                attrs.put("scheduledEventId", event.getActivityTaskCompletedEventAttributes().getScheduledEventId());
            }
            case ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES -> {
                var a = event.getActivityTaskFailedEventAttributes();
                if (a.hasFailure()) attrs.put("message", a.getFailure().getMessage());
                attrs.put("scheduledEventId", a.getScheduledEventId());
            }
            case ACTIVITY_TASK_TIMED_OUT_EVENT_ATTRIBUTES -> {
                var a = event.getActivityTaskTimedOutEventAttributes();
                attrs.put("retryState", a.getRetryState().name());
                attrs.put("scheduledEventId", a.getScheduledEventId());
            }
            case TIMER_STARTED_EVENT_ATTRIBUTES -> {
                var a = event.getTimerStartedEventAttributes();
                attrs.put("timerId", a.getTimerId());
                if (a.hasStartToFireTimeout()) {
                    attrs.put("startToFireSeconds", a.getStartToFireTimeout().getSeconds());
                }
            }
            case TIMER_FIRED_EVENT_ATTRIBUTES -> {
                attrs.put("timerId", event.getTimerFiredEventAttributes().getTimerId());
            }
            case TIMER_CANCELED_EVENT_ATTRIBUTES -> {
                attrs.put("timerId", event.getTimerCanceledEventAttributes().getTimerId());
            }
            case START_CHILD_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES -> {
                var a = event.getStartChildWorkflowExecutionInitiatedEventAttributes();
                attrs.put("workflowId", a.getWorkflowId());
                attrs.put("workflowType", a.getWorkflowType().getName());
                attrs.put("taskQueue", a.getTaskQueue().getName());
            }
            case CHILD_WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES -> {
                var a = event.getChildWorkflowExecutionStartedEventAttributes();
                attrs.put("workflowId", a.getWorkflowExecution().getWorkflowId());
                attrs.put("workflowType", a.getWorkflowType().getName());
            }
            case CHILD_WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES -> {
                var a = event.getChildWorkflowExecutionCompletedEventAttributes();
                attrs.put("workflowId", a.getWorkflowExecution().getWorkflowId());
                attrs.put("workflowType", a.getWorkflowType().getName());
            }
            case CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES -> {
                var a = event.getChildWorkflowExecutionFailedEventAttributes();
                attrs.put("workflowId", a.getWorkflowExecution().getWorkflowId());
                attrs.put("workflowType", a.getWorkflowType().getName());
                if (a.hasFailure()) attrs.put("message", a.getFailure().getMessage());
            }
            case CHILD_WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES -> {
                var a = event.getChildWorkflowExecutionTerminatedEventAttributes();
                attrs.put("workflowId", a.getWorkflowExecution().getWorkflowId());
            }
            default -> {}
        }
        return attrs;
    }

    private Map<String, Object> toInfoMap(WorkflowExecutionInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("workflowId", info.getExecution().getWorkflowId());
        map.put("runId", info.getExecution().getRunId());
        map.put("workflowType", info.getType().getName());
        // Prefer customStatus search attribute (e.g. "PAUSED") over the native Temporal status
        String nativeStatus = toStatusString(info.getStatus());
        String displayStatus = nativeStatus;
        if ("RUNNING".equals(nativeStatus) && info.hasSearchAttributes()) {
            var saMap = info.getSearchAttributes().getIndexedFieldsMap();
            if (saMap.containsKey("customStatus")) {
                String raw = saMap.get("customStatus").getData().toStringUtf8();
                if (raw.startsWith("\"") && raw.endsWith("\"")) raw = raw.substring(1, raw.length() - 1);
                displayStatus = raw;
            }
        }
        map.put("status", displayStatus);
        map.put("taskQueue", info.getTaskQueue());
        if (info.hasStartTime()) {
            map.put("startTime", Instant.ofEpochSecond(
                    info.getStartTime().getSeconds(),
                    info.getStartTime().getNanos()).toString());
        }
        if (info.hasCloseTime()) {
            map.put("closeTime", Instant.ofEpochSecond(
                    info.getCloseTime().getSeconds(),
                    info.getCloseTime().getNanos()).toString());
        }
        if (info.hasMemo() && !info.getMemo().getFieldsMap().isEmpty()) {
            map.put("memo", extractMemo(info.getMemo()));
        }
        return map;
    }

    /**
     * Decode Memo fields from Temporal Payload bytes.
     * Payload data is JSON-encoded, e.g. bytes of `"health-check-workflow-v2"` (with quotes).
     * Strip surrounding quotes for string values.
     */
    private Map<String, Object> extractMemo(Memo memo) {
        Map<String, Object> result = new LinkedHashMap<>();
        memo.getFieldsMap().forEach((key, payload) -> {
            String raw = payload.getData().toStringUtf8();
            // Strip JSON string quotes if present
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                raw = raw.substring(1, raw.length() - 1);
            }
            result.put(key, raw);
        });
        return result;
    }

    private static String toStatusString(WorkflowExecutionStatus status) {
        return switch (status) {
            case WORKFLOW_EXECUTION_STATUS_RUNNING -> "RUNNING";
            case WORKFLOW_EXECUTION_STATUS_COMPLETED -> "COMPLETED";
            case WORKFLOW_EXECUTION_STATUS_FAILED -> "FAILED";
            case WORKFLOW_EXECUTION_STATUS_CANCELED -> "CANCELED";
            case WORKFLOW_EXECUTION_STATUS_TERMINATED -> "TERMINATED";
            case WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW -> "CONTINUED_AS_NEW";
            case WORKFLOW_EXECUTION_STATUS_TIMED_OUT -> "TIMED_OUT";
            default -> status.name();
        };
    }
}
