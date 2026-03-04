package com.example.temporaldemo.healthcheck.activity;

import com.example.temporaldemo.healthcheck.model.DiagnosisResult;
import com.example.temporaldemo.healthcheck.model.FollowUpRecord;
import com.example.temporaldemo.healthcheck.model.PatientVisit;
import com.example.temporaldemo.healthcheck.model.Severity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface HealthCheckActivities {

    @ActivityMethod
    String recordVisit(PatientVisit visit);

    @ActivityMethod
    DiagnosisResult getDiagnosisResult(String patientId, String scenario);

    @ActivityMethod
    Severity analyzeResults(DiagnosisResult diagnosisResult);

    @ActivityMethod
    FollowUpRecord performFollowUp(String patientId, int sequenceNumber, Severity severity, double previousScore);

    @ActivityMethod
    void notifyDoctor(String doctorName, String patientId, DiagnosisResult diagnosisResult);
}
