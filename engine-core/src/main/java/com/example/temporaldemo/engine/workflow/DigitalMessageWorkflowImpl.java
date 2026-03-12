package com.example.temporaldemo.engine.workflow;

import com.example.temporaldemo.engine.digitalmessage.DigitalMessageRequest;
import com.example.temporaldemo.engine.digitalmessage.DigitalMessageResult;
import com.example.temporaldemo.engine.digitalmessage.DigitalMessageWorkflow;
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
 * Child workflow implementation for DIGITAL_MESSAGE node coordination.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Send notifications in parallel via all configured channels.</li>
 *   <li>Wait for {@link #digitalMessageSignal(String)} with a configurable timeout.</li>
 *   <li>Return {@link DigitalMessageResult} to the parent workflow.</li>
 * </ol>
 */
public class DigitalMessageWorkflowImpl implements DigitalMessageWorkflow {

    private static final Logger logger = Workflow.getLogger(DigitalMessageWorkflowImpl.class);
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

    private String pendingAction = null;
    private String taskId;
    private String status = "WAITING";
    private boolean dmPaused = false;

    // ─── WorkflowMethod ──────────────────────────────────────────────────────

    @Override
    public DigitalMessageResult execute(DigitalMessageRequest request) {
        this.taskId = request.getTaskId();
        int timeoutSecs = request.getTimeoutSeconds() > 0
                ? request.getTimeoutSeconds()
                : DEFAULT_TIMEOUT_SECONDS;

        logger.info("DigitalMessage [{}] started. Parent: {}, channels: {}, timeout: {}s",
                taskId, request.getParentWorkflowId(), request.getChannels(), timeoutSecs);

        // ── Step 1: Send notifications in parallel ────────────────────────
        List<Promise<Object>> promises = new ArrayList<>();

        List<String> channels = request.getChannels() != null
                ? request.getChannels()
                : Collections.emptyList();

        for (String channel : channels) {
            Map<String, Object> notifInput = buildNotificationInput(request, channel);
            switch (channel.toUpperCase()) {
                case "EMAIL" -> promises.add(
                        Async.function(() -> activityStub.execute("sendNotificationEmail", Object.class, notifInput)));
                case "SMS" -> promises.add(
                        Async.function(() -> activityStub.execute("sendNotificationSms", Object.class, notifInput)));
                case "PHONE" -> promises.add(
                        Async.function(() -> activityStub.execute("sendNotificationCall", Object.class, notifInput)));
                default -> logger.warn("DigitalMessage [{}]: unknown channel '{}', skipping", taskId, channel);
            }
        }

        // Wait for all notifications before entering wait state
        for (Promise<Object> p : promises) {
            try {
                p.get();
            } catch (Exception e) {
                logger.warn("DigitalMessage [{}]: notification failed: {}", taskId, e.getMessage());
            }
        }

        logger.info("DigitalMessage [{}]: notifications sent. Now waiting for signal (timeout={}s).", taskId, timeoutSecs);

        // ── Step 2: Wait for signal with timeout (pause suspends countdown) ──
        long remainingMs = (long) timeoutSecs * 1000;
        while (pendingAction == null && remainingMs > 0) {
            if (dmPaused) {
                // Suspended — wait until unpaused or action received, don't consume timeout
                logger.info("DigitalMessage [{}]: paused, suspending timeout countdown.", taskId);
                Workflow.await(() -> !dmPaused || pendingAction != null);
                logger.info("DigitalMessage [{}]: resumed or action received.", taskId);
            } else {
                long waitStart = Workflow.currentTimeMillis();
                Workflow.await(Duration.ofMillis(remainingMs), () -> pendingAction != null || dmPaused);
                remainingMs -= (Workflow.currentTimeMillis() - waitStart);
            }
        }

        // ── Step 3: Resolve ───────────────────────────────────────────────
        if (pendingAction == null) {
            logger.info("DigitalMessage [{}]: timed out after {}s — auto-approved.", taskId, timeoutSecs);
            status = "TIMEOUT_AUTO_APPROVED";
            return new DigitalMessageResult("TIMEOUT_AUTO_APPROVED", "Task timed out and was auto-approved.");
        }

        String action = pendingAction;
        logger.info("DigitalMessage [{}]: resolved with action='{}'", taskId, action);

        return switch (action.toLowerCase()) {
            case "execute" -> {
                status = "EXECUTED";
                yield new DigitalMessageResult("EXECUTED", "Digital message was approved.");
            }
            case "skip" -> {
                status = "SKIPPED";
                yield new DigitalMessageResult("SKIPPED", "Digital message was skipped.");
            }
            case "terminate" -> {
                status = "TERMINATED";
                yield new DigitalMessageResult("TERMINATED", "Workflow was terminated via digital message.");
            }
            case "cancel" -> {
                status = "CANCELED";
                yield new DigitalMessageResult("CANCELED", "Digital message was cancelled.");
            }
            default -> {
                logger.warn("DigitalMessage [{}]: unrecognised action '{}', defaulting to execute.", taskId, action);
                status = "EXECUTED";
                yield new DigitalMessageResult("EXECUTED", "Unknown action '" + action + "', defaulted to execute.");
            }
        };
    }

    // ─── SignalMethod ─────────────────────────────────────────────────────────

    @Override
    public void digitalMessageSignal(String action) {
        logger.info("DigitalMessage [{}]: signal received, action='{}'", taskId, action);
        this.pendingAction = action;
    }

    @Override
    public void pauseDigitalMessage() {
        logger.info("DigitalMessage [{}]: pause signal received.", taskId);
        this.dmPaused = true;
        this.status = "PAUSED";
    }

    @Override
    public void resumeDigitalMessage() {
        logger.info("DigitalMessage [{}]: resume signal received.", taskId);
        this.dmPaused = false;
        this.status = "WAITING";
    }

    // ─── QueryMethod ─────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getTaskStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId", taskId);
        map.put("status", status);
        map.put("paused", dmPaused);
        map.put("workflowId", Workflow.getInfo().getWorkflowId());
        return map;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Map<String, Object> buildNotificationInput(DigitalMessageRequest request, String channel) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("taskId", request.getTaskId());
        input.put("taskName", request.getTaskName() != null ? request.getTaskName() : request.getTaskId());
        input.put("workflowId", request.getParentWorkflowId());

        switch (channel.toUpperCase()) {
            case "EMAIL" -> {
                input.put("to", request.getTo());
                if (request.getEmailConfig() != null) {
                    if (request.getEmailConfig().getFromEmail() != null)
                        input.put("fromEmail", request.getEmailConfig().getFromEmail());
                    if (request.getEmailConfig().getCc() != null)
                        input.put("cc", request.getEmailConfig().getCc());
                    if (request.getEmailConfig().getBcc() != null)
                        input.put("bcc", request.getEmailConfig().getBcc());
                    if (request.getEmailConfig().getMessage() != null)
                        input.put("message", request.getEmailConfig().getMessage());
                } else if (request.getMessage() != null) {
                    input.put("message", request.getMessage());
                }
            }
            case "SMS", "PHONE" -> {
                input.put("phone", request.getPhone());
                if (request.getSmsConfig() != null) {
                    if (request.getSmsConfig().getFromPhone() != null)
                        input.put("fromPhone", request.getSmsConfig().getFromPhone());
                    if (request.getSmsConfig().getMessage() != null)
                        input.put("message", request.getSmsConfig().getMessage());
                } else if (request.getMessage() != null) {
                    input.put("message", request.getMessage());
                }
            }
            default -> {
                if (request.getTo() != null)    input.put("to", request.getTo());
                if (request.getPhone() != null) input.put("phone", request.getPhone());
                if (request.getMessage() != null) input.put("message", request.getMessage());
            }
        }
        return input;
    }
}
