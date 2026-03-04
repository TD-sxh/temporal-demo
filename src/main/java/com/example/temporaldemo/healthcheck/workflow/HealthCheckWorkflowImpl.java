package com.example.temporaldemo.healthcheck.workflow;

import com.example.temporaldemo.healthcheck.activity.HealthCheckActivities;
import com.example.temporaldemo.healthcheck.model.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HealthCheckWorkflowImpl implements HealthCheckWorkflow {

    private static final Logger logger = Workflow.getLogger(HealthCheckWorkflowImpl.class);

    private final HealthCheckActivities activities = Workflow.newActivityStub(
            HealthCheckActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .build())
                    .build());

    // --- Workflow state (readable via Query, mutated via Signals) ---
    private String patientId;
    private Severity currentSeverity;
    private int followUpCompleted = 0;
    private int maxFollowUps = 0;
    private boolean cancelled = false;
    private String cancelReason = "";
    private String currentPhase = "INIT";
    private final Queue<Double> pendingLabScores = new LinkedList<>();

    // --- Query ---
    @Override
    public HealthCheckStatus getStatus() {
        return new HealthCheckStatus(patientId, currentSeverity, followUpCompleted,
                maxFollowUps, cancelled, currentPhase);
    }

    // --- Signals ---
    @Override
    public void cancelFollowUp(String reason) {
        logger.info("Signal received: cancelFollowUp. Reason: {}", reason);
        this.cancelled = true;
        this.cancelReason = reason;
    }

    @Override
    public void newLabResult(double newScore) {
        logger.info("Signal received: newLabResult. New score: {}", newScore);
        this.pendingLabScores.add(newScore);
    }

    // --- Main workflow ---
    @Override
    public String processHealthCheck(PatientVisit visit) {
        this.patientId = visit.getPatientId();

        // Step 1: Record the visit
        currentPhase = "RECORDING_VISIT";
        logger.info("Step 1: Recording visit for patient {}", patientId);
        String visitId = activities.recordVisit(visit);

        // Step 2: Get diagnosis results
        currentPhase = "GETTING_DIAGNOSIS";
        logger.info("Step 2: Getting diagnosis for patient {}", patientId);
        DiagnosisResult diagnosis = activities.getDiagnosisResult(patientId, visit.getScenario());
        double currentScore = diagnosis.getScore();

        // Step 3: Analyze results
        currentPhase = "ANALYZING";
        logger.info("Step 3: Analyzing results for patient {}", patientId);
        currentSeverity = activities.analyzeResults(diagnosis);

        // Step 4: Follow-up loop with feedback
        currentPhase = "FOLLOW_UP";
        initFollowUpPlan();
        logger.info("Step 4: Severity={}, maxFollowUps={}. Starting follow-up loop.", currentSeverity, maxFollowUps);

        // Notify doctor immediately for SEVERE cases
        if (currentSeverity == Severity.SEVERE) {
            logger.info("SEVERE: Immediately notifying {}", visit.getDoctorName());
            activities.notifyDoctor(visit.getDoctorName(), patientId, diagnosis);
        }

        List<FollowUpRecord> followUps = new ArrayList<>();

        while (followUpCompleted < maxFollowUps && !cancelled) {
            // Wait between follow-ups (Signal-aware: wakes immediately on cancel or new lab result)
            Duration interval = getFollowUpInterval();
            logger.info("Waiting {} before follow-up #{}", interval, followUpCompleted + 1);
            Workflow.await(interval, () -> cancelled || !pendingLabScores.isEmpty());

            // Check if cancelled during wait
            if (cancelled) {
                logger.info("Follow-up cancelled. Reason: {}", cancelReason);
                break;
            }

            // Process all pending lab results from Signals (use latest score)
            if (!pendingLabScores.isEmpty()) {
                while (!pendingLabScores.isEmpty()) {
                    currentScore = pendingLabScores.poll();
                    logger.info("Lab result from Signal applied: score={}", currentScore);
                }
                Severity newSeverity = Severity.fromScore(currentScore);
                if (newSeverity != currentSeverity) {
                    logger.info("Severity changed: {} -> {}", currentSeverity, newSeverity);
                    Severity oldSeverity = currentSeverity;
                    currentSeverity = newSeverity;
                    initFollowUpPlan();
                    if (newSeverity == Severity.SEVERE && oldSeverity != Severity.SEVERE) {
                        logger.info("Escalating to SEVERE! Notifying {}", visit.getDoctorName());
                        activities.notifyDoctor(visit.getDoctorName(), patientId, diagnosis);
                    }
                }
            }

            // Perform follow-up
            followUpCompleted++;
            FollowUpRecord record = activities.performFollowUp(patientId, followUpCompleted, currentSeverity, currentScore);
            followUps.add(record);
            currentScore = record.getHealthScore();

            // Evaluate follow-up result: dynamic escalation/de-escalation
            Severity newSeverity = Severity.fromScore(currentScore);
            if (newSeverity != currentSeverity) {
                Severity oldSeverity = currentSeverity;
                logger.info("Follow-up #{} caused severity change: {} -> {}", followUpCompleted, oldSeverity, newSeverity);
                currentSeverity = newSeverity;
                initFollowUpPlan();

                if (newSeverity == Severity.SEVERE && oldSeverity != Severity.SEVERE) {
                    logger.info("Escalating to SEVERE! Notifying {}", visit.getDoctorName());
                    activities.notifyDoctor(visit.getDoctorName(), patientId, diagnosis);
                }
            }

            // Early exit: if patient recovered to NORMAL and at least 1 follow-up done
            if (currentSeverity == Severity.NORMAL && followUpCompleted >= 3) {
                logger.info("Patient recovered to NORMAL after {} follow-ups. Ending early.", followUpCompleted);
                break;
            }
        }

        // Step 5: Build summary
        currentPhase = cancelled ? "CANCELLED" : "COMPLETED";
        String summary = String.format(
                "Health check %s for patient %s (%s). Visit ID: %s. " +
                "Diagnosis: %s (initial score: %.2f, final score: %.2f). " +
                "Final severity: %s. Follow-ups performed: %d.%s",
                cancelled ? "CANCELLED" : "COMPLETE",
                patientId, visit.getPatientName(), visitId,
                diagnosis.getDiagnosisDescription(), diagnosis.getScore(), currentScore,
                currentSeverity, followUps.size(),
                cancelled ? " Cancel reason: " + cancelReason : "");

        logger.info("Workflow done: {}", summary);
        return summary;
    }

    private void initFollowUpPlan() {
        switch (currentSeverity) {
            case NORMAL:
                maxFollowUps = Math.max(followUpCompleted + 1, 3);
                break;
            case ABNORMAL:
                maxFollowUps = Math.max(followUpCompleted + 3, 5);
                break;
            case SEVERE:
                maxFollowUps = Math.max(followUpCompleted + 5, 7);
                break;
        }
    }

    private Duration getFollowUpInterval() {
        return switch (currentSeverity) {
            case SEVERE -> Duration.ofSeconds(15);  // represents 3 days
            case ABNORMAL -> Duration.ofSeconds(20);  // represents 7 days
            default -> Duration.ofSeconds(30);  // represents 7 days
        };
    }
}
