package com.example.temporaldemo.worker.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Strongly-typed Temporal Activity interface for Health Check workflows.
 *
 * <p>Each method name matches the {@code activityName} in the JSON workflow definition,
 * so the engine's {@code UntypedActivityStub.execute("recordVisit", ...)} routes here.
 */
@ActivityInterface
public interface HealthCheckActivities {

    @ActivityMethod
    @ActivityMeta(description = "Records a patient visit",
            inputKeys = {"patientId", "patientName", "doctorName", "visitReason"})
    Object recordVisit(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Fetches diagnosis result",
            inputKeys = {"patientId", "scenario"})
    Object getDiagnosisResult(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Analyzes diagnosis results",
            inputKeys = {"diagnosis"})
    Object analyzeResults(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Notifies the doctor",
            inputKeys = {"doctorName", "patientId", "severity"})
    Object notifyDoctor(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Sends an email notification",
            inputKeys = {"doctorName", "patientId", "diagnosis"})
    Object sendEmail(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Sends an SMS notification",
            inputKeys = {"doctorName", "patientId", "severity"})
    Object sendSms(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Processes a lab signal",
            inputKeys = {"labSignal", "currentScore"})
    Object processLabSignal(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Performs a follow-up action",
            inputKeys = {"patientId", "followUpCount", "severity", "currentScore"})
    Object performFollowUp(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Updates follow-up counters",
            inputKeys = {"followUpResult", "followUpCount"})
    Object updateFollowUpState(Map<String, Object> input);

    @ActivityMethod
    @ActivityMeta(description = "Builds a workflow summary",
            inputKeys = {"patientId", "severity", "followUpCount", "visitId"})
    Object buildSummary(Map<String, Object> input);
}
