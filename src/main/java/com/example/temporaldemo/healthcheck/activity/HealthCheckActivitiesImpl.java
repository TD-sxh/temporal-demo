package com.example.temporaldemo.healthcheck.activity;

import com.example.temporaldemo.healthcheck.model.DiagnosisResult;
import com.example.temporaldemo.healthcheck.model.FollowUpRecord;
import com.example.temporaldemo.healthcheck.model.PatientVisit;
import com.example.temporaldemo.healthcheck.model.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Random;

public class HealthCheckActivitiesImpl implements HealthCheckActivities {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckActivitiesImpl.class);

    @Override
    public String recordVisit(PatientVisit visit) {
        String visitId = "VISIT-" + System.currentTimeMillis();
        logger.info("[recordVisit] Recorded visit {} for patient {} ({}) with {}. Reason: {}",
                visitId, visit.getPatientId(), visit.getPatientName(),
                visit.getDoctorName(), visit.getVisitReason());
        return visitId;
    }

    @Override
    public DiagnosisResult getDiagnosisResult(String patientId, String scenario) {
        double score;
        if ("normal".equalsIgnoreCase(scenario)) {
            score = 0.2;
        } else if ("abnormal".equalsIgnoreCase(scenario)) {
            score = 0.6;
        } else if ("severe".equalsIgnoreCase(scenario)) {
            score = 0.85;
        } else {
            score = new Random().nextDouble();
        }

        String code;
        String description;
        if (score < 0.4) {
            code = "D-NORMAL-001";
            description = "All vitals within normal range";
        } else if (score < 0.75) {
            code = "D-ABNORMAL-002";
            description = "Elevated blood pressure and cholesterol";
        } else {
            code = "D-SEVERE-003";
            description = "Critical cardiac enzyme levels detected";
        }
        logger.info("[getDiagnosisResult] Patient {} (scenario={}): score={}, code={}, desc={}",
                patientId, scenario, String.format("%.2f", score), code, description);
        return new DiagnosisResult(patientId, code, description, score);
    }

    @Override
    public Severity analyzeResults(DiagnosisResult diagnosisResult) {
        double score = diagnosisResult.getScore();
        Severity severity = Severity.fromScore(score);
        logger.info("[analyzeResults] Patient {}: score={} -> {}",
                diagnosisResult.getPatientId(), String.format("%.2f", score), severity);
        return severity;
    }

    @Override
    public FollowUpRecord performFollowUp(String patientId, int sequenceNumber, Severity severity, double previousScore) {
        // Simulate health score change: patient generally improves over follow-ups
        // but with some randomness
        double delta = (new Random().nextDouble() - 0.3) * 0.2; // tends to improve (decrease score)
        double newScore = Math.max(0.0, Math.min(1.0, previousScore + delta));

        String trend;
        if (newScore < previousScore - 0.05) {
            trend = "IMPROVING";
        } else if (newScore > previousScore + 0.05) {
            trend = "WORSENING";
        } else {
            trend = "STABLE";
        }

        String notes = String.format("Follow-up #%d for %s case. Previous score: %.2f -> New score: %.2f (%s)",
                sequenceNumber, severity, previousScore, newScore, trend);
        String timestamp = Instant.now().toString();

        logger.info("[performFollowUp] Patient {}: #{} | {} -> {} ({}) at {}",
                patientId, sequenceNumber, String.format("%.2f", previousScore),
                String.format("%.2f", newScore), trend, timestamp);
        return new FollowUpRecord(patientId, sequenceNumber, notes, timestamp, newScore);
    }

    @Override
    public void notifyDoctor(String doctorName, String patientId, DiagnosisResult diagnosisResult) {
        logger.info("[notifyDoctor] URGENT: {} notified about patient {}! Diagnosis: {} (score: {})",
                doctorName, patientId,
                diagnosisResult.getDiagnosisDescription(),
                String.format("%.2f", diagnosisResult.getScore()));
    }
}
