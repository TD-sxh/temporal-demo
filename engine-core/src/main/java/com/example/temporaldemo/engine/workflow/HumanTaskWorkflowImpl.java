package com.example.temporaldemo.engine.workflow;

import com.example.temporaldemo.engine.humantask.HumanTaskRequest;
import com.example.temporaldemo.engine.humantask.HumanTaskResult;
import com.example.temporaldemo.engine.humantask.HumanTaskWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

/**
 * Child workflow implementation for human task coordination.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Send notifications in parallel via all configured channels.</li>
 *   <li>Wait for {@link #humanTaskSignal(String)} with a configurable timeout.</li>
 *   <li>Return {@link HumanTaskResult} to the parent workflow.</li>
 * </ol>
 *
 * <p>This workflow runs on the same task queue as {@code OrchestratorWorkflowImpl}
 * ({@code orchestrator-task-queue}). Notification activities are dispatched to the
 * health-check-worker on the same queue.
 */
public class HumanTaskWorkflowImpl implements HumanTaskWorkflow {

    private static final Logger logger = Workflow.getLogger(HumanTaskWorkflowImpl.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 3600;

    private final ActivityStub activityStub = Workflow.newUntypedActivityStub(
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(2)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .build())
                    .build());

    // ─── State ───────────────────────────────────────────────────────────────

    /** Set by humanTaskSignal(); read inside Workflow.await(). */
    private String pendingAction = null;

    private String taskId;
    private String status = "WAITING";

    // ─── WorkflowMethod ──────────────────────────────────────────────────────

    @Override
    public HumanTaskResult execute(HumanTaskRequest request) {
        this.taskId = request.getTaskId();
        int timeoutSecs = request.getTimeoutSeconds() > 0
                ? request.getTimeoutSeconds()
                : DEFAULT_TIMEOUT_SECONDS;

        logger.info("HumanTask [{}] started. Parent: {}, channels: {}, timeout: {}s",
                taskId, request.getParentWorkflowId(), request.getChannels(), timeoutSecs);

        // ── Step 1: Send notifications in parallel ────────────────────────
        Map<String, Object> notifInput = buildNotificationInput(request);
        List<Promise<Object>> promises = new ArrayList<>();

        List<String> channels = request.getChannels() != null
                ? request.getChannels()
                : Collections.emptyList();

        for (String channel : channels) {
            switch (channel.toUpperCase()) {
                case "EMAIL" -> promises.add(
                        Async.function(() -> activityStub.execute("sendNotificationEmail", Object.class, notifInput)));
                case "SMS" -> promises.add(
                        Async.function(() -> activityStub.execute("sendNotificationSms", Object.class, notifInput)));
                case "PHONE" -> promises.add(
                        Async.function(() -> activityStub.execute("sendNotificationCall", Object.class, notifInput)));
                default -> logger.warn("HumanTask [{}]: unknown channel '{}', skipping", taskId, channel);
            }
        }

        // Wait for all notifications to complete before entering wait state
        for (Promise<Object> p : promises) {
            try {
                p.get();
            } catch (Exception e) {
                logger.warn("HumanTask [{}]: notification failed: {}", taskId, e.getMessage());
            }
        }

        logger.info("HumanTask [{}]: notifications sent. Now waiting for signal (timeout={}s).", taskId, timeoutSecs);

        // ── Step 2: Wait for signal with timeout ──────────────────────────
        boolean signaled = Workflow.await(
                Duration.ofSeconds(timeoutSecs),
                () -> pendingAction != null);

        // ── Step 3: Resolve ───────────────────────────────────────────────
        if (!signaled || pendingAction == null) {
            logger.info("HumanTask [{}]: timed out after {}s — auto-approved.", taskId, timeoutSecs);
            status = "TIMEOUT_AUTO_APPROVED";
            return new HumanTaskResult("TIMEOUT_AUTO_APPROVED", "Task timed out and was auto-approved.");
        }

        String action = pendingAction;
        logger.info("HumanTask [{}]: resolved with action='{}'", taskId, action);

        return switch (action.toLowerCase()) {
            case "execute" -> {
                status = "EXECUTED";
                yield new HumanTaskResult("EXECUTED", "Task was manually executed.");
            }
            case "skip" -> {
                status = "SKIPPED";
                yield new HumanTaskResult("SKIPPED", "Task was skipped.");
            }
            case "terminate" -> {
                status = "TERMINATED";
                yield new HumanTaskResult("TERMINATED", "Workflow was terminated by human task.");
            }
            default -> {
                logger.warn("HumanTask [{}]: unrecognised action '{}', defaulting to execute.", taskId, action);
                status = "EXECUTED";
                yield new HumanTaskResult("EXECUTED", "Unknown action '" + action + "', defaulted to execute.");
            }
        };
    }

    // ─── SignalMethod ─────────────────────────────────────────────────────────

    @Override
    public void humanTaskSignal(String action) {
        logger.info("HumanTask [{}]: signal received, action='{}'", taskId, action);
        this.pendingAction = action;
    }

    // ─── QueryMethod ─────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getTaskStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId", taskId);
        map.put("status", status);
        map.put("workflowId", Workflow.getInfo().getWorkflowId());
        return map;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Map<String, Object> buildNotificationInput(HumanTaskRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("taskId", request.getTaskId());
        input.put("taskName", request.getTaskName() != null ? request.getTaskName() : request.getTaskId());
        input.put("workflowId", request.getParentWorkflowId());
        if (request.getTo() != null)      input.put("to", request.getTo());
        if (request.getPhone() != null)   input.put("phone", request.getPhone());
        if (request.getMessage() != null) input.put("message", request.getMessage());
        return input;
    }
}
