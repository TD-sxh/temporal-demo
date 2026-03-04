package com.example.temporaldemo.healthcheck.workflow;

import com.example.temporaldemo.healthcheck.model.HealthCheckStatus;
import com.example.temporaldemo.healthcheck.model.PatientVisit;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HealthCheckWorkflow {

    @WorkflowMethod
    String processHealthCheck(PatientVisit visit);

    // --- Signals: external events injected into a running workflow ---

    @SignalMethod
    void cancelFollowUp(String reason);

    @SignalMethod
    void newLabResult(double newScore);

    // --- Query: read workflow state without affecting execution ---

    @QueryMethod
    HealthCheckStatus getStatus();
}
