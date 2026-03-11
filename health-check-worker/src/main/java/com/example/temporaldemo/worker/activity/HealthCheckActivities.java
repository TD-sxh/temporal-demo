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
    Object recordVisit(Map<String, Object> input);

    @ActivityMethod
    Object getDiagnosisResult(Map<String, Object> input);

    @ActivityMethod
    Object analyzeResults(Map<String, Object> input);

    @ActivityMethod
    Object notifyDoctor(Map<String, Object> input);

    @ActivityMethod
    Object sendEmail(Map<String, Object> input);

    @ActivityMethod
    Object sendSms(Map<String, Object> input);

    @ActivityMethod
    Object processLabSignal(Map<String, Object> input);

    @ActivityMethod
    Object performFollowUp(Map<String, Object> input);

    @ActivityMethod
    Object updateFollowUpState(Map<String, Object> input);

    @ActivityMethod
    Object buildSummary(Map<String, Object> input);
}
