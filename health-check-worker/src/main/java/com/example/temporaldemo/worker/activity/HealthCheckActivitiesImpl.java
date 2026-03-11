package com.example.temporaldemo.worker.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of all health-check activities.
 * Each method contains the business logic previously in separate Handler classes.
 */
public class HealthCheckActivitiesImpl implements HealthCheckActivities {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckActivitiesImpl.class);

    // ─── recordVisit ────────────────────────────────

    @Override
    public Object recordVisit(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        String patientName = (String) input.get("patientName");
        String doctorName = (String) input.get("doctorName");
        String visitReason = (String) input.get("visitReason");

        String visitId = "VISIT-" + System.currentTimeMillis();
        logger.info("[recordVisit] Recorded visit {} for patient {} ({}) with {}. Reason: {}",
                visitId, patientId, patientName, doctorName, visitReason);
        return visitId;
    }

    // ─── getDiagnosisResult ─────────────────────────

    @Override
    public Object getDiagnosisResult(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        String scenario = (String) input.get("scenario");

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

        logger.info("[getDiagnosisResult] Patient {} (scenario={}): score={}, code={}",
                patientId, scenario, String.format("%.2f", score), code);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("code", code);
        result.put("description", description);
        result.put("score", score);
        return result;
    }

    // ─── analyzeResults ─────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Object analyzeResults(Map<String, Object> input) {
        Object diagnosisObj = input.get("diagnosis");
        double score;
        if (diagnosisObj instanceof Map) {
            Map<String, Object> diagnosis = (Map<String, Object>) diagnosisObj;
            score = ((Number) diagnosis.get("score")).doubleValue();
        } else {
            score = 0.5;
        }

        String severity;
        if (score < 0.4) {
            severity = "NORMAL";
        } else if (score < 0.75) {
            severity = "ABNORMAL";
        } else {
            severity = "SEVERE";
        }

        logger.info("[analyzeResults] score={} -> {}", String.format("%.2f", score), severity);
        return severity;
    }

    // ─── notifyDoctor ───────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Object notifyDoctor(Map<String, Object> input) {
        String doctorName = (String) input.get("doctorName");
        String patientId = (String) input.get("patientId");
        Object diagnosisObj = input.get("diagnosis");

        String description = "N/A";
        String score = "N/A";
        if (diagnosisObj instanceof Map) {
            Map<String, Object> diagnosis = (Map<String, Object>) diagnosisObj;
            description = (String) diagnosis.getOrDefault("description", "N/A");
            Object s = diagnosis.get("score");
            score = s != null ? String.format("%.2f", ((Number) s).doubleValue()) : "N/A";
        }

        logger.info("[notifyDoctor] URGENT: Dr. {} notified about patient {}! Diagnosis: {} (score: {})",
                doctorName, patientId, description, score);
        return "NOTIFIED";
    }

    // ─── sendEmail ──────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Object sendEmail(Map<String, Object> input) {
        String doctorName = (String) input.get("doctorName");
        String patientId = (String) input.get("patientId");
        Object diagnosisObj = input.get("diagnosis");

        String description = "N/A";
        if (diagnosisObj instanceof Map) {
            Map<String, Object> diagnosis = (Map<String, Object>) diagnosisObj;
            description = (String) diagnosis.getOrDefault("description", "N/A");
        }

        logger.info("[sendEmail] Sending email to Dr. {} about patient {} — {}", doctorName, patientId, description);

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return Map.of(
                "channel", "EMAIL",
                "recipient", doctorName,
                "status", "SENT",
                "message", "Urgent: Patient " + patientId + " diagnosis — " + description
        );
    }

    // ─── sendSms ────────────────────────────────────

    @Override
    public Object sendSms(Map<String, Object> input) {
        String doctorName = (String) input.get("doctorName");
        String patientId = (String) input.get("patientId");
        String severity = (String) input.get("severity");

        logger.info("[sendSms] Sending SMS to Dr. {} — patient {} severity: {}", doctorName, patientId, severity);

        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return Map.of(
                "channel", "SMS",
                "recipient", doctorName,
                "status", "SENT",
                "message", "ALERT: Patient " + patientId + " — " + severity
        );
    }

    // ─── processLabSignal ───────────────────────────

    @Override
    public Object processLabSignal(Map<String, Object> input) {
        Object labSignal = input.get("labSignal");
        Object currentScoreObj = input.get("currentScore");
        double currentScore = currentScoreObj instanceof Number ? ((Number) currentScoreObj).doubleValue() : 0.0;

        if (labSignal instanceof Number) {
            double newScore = ((Number) labSignal).doubleValue();
            logger.info("[processLabSignal] Lab signal applied: {} -> {}",
                    String.format("%.2f", currentScore), String.format("%.2f", newScore));
            return newScore;
        }

        logger.info("[processLabSignal] No lab signal, keeping current score: {}", String.format("%.2f", currentScore));
        return currentScore;
    }

    // ─── performFollowUp ────────────────────────────

    @Override
    public Object performFollowUp(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        int followUpCount = toInt(input.get("followUpCount"));
        int sequenceNumber = followUpCount + 1;
        String severity = (String) input.get("severity");
        double previousScore = toDouble(input.get("currentScore"));

        double delta = (new Random().nextDouble() - 0.3) * 0.2;
        double newScore = Math.max(0.0, Math.min(1.0, previousScore + delta));

        String trend;
        if (newScore < previousScore - 0.05) {
            trend = "IMPROVING";
        } else if (newScore > previousScore + 0.05) {
            trend = "WORSENING";
        } else {
            trend = "STABLE";
        }

        String notes = String.format("Follow-up #%d for %s case. Previous: %.2f -> New: %.2f (%s)",
                sequenceNumber, severity, previousScore, newScore, trend);
        String timestamp = Instant.now().toString();

        logger.info("[performFollowUp] Patient {}: #{} | {} -> {} ({}) at {}",
                patientId, sequenceNumber, String.format("%.2f", previousScore),
                String.format("%.2f", newScore), trend, timestamp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("sequenceNumber", sequenceNumber);
        result.put("notes", notes);
        result.put("timestamp", timestamp);
        result.put("healthScore", newScore);
        return result;
    }

    // ─── updateFollowUpState ────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Object updateFollowUpState(Map<String, Object> input) {
        Object followUpResultObj = input.get("followUpResult");
        int followUpCount = toInt(input.get("followUpCount"));

        int newCount = followUpCount + 1;
        double newScore = 0.0;

        if (followUpResultObj instanceof Map) {
            Map<String, Object> result = (Map<String, Object>) followUpResultObj;
            newScore = toDouble(result.get("healthScore"));
        }

        String severity;
        int maxFollowUps;
        if (newScore < 0.4) {
            severity = "NORMAL";
            maxFollowUps = 3;
        } else if (newScore < 0.75) {
            severity = "ABNORMAL";
            maxFollowUps = 5;
        } else {
            severity = "SEVERE";
            maxFollowUps = 7;
        }

        logger.info("[updateFollowUpState] count={}, score={}, severity={}, maxFollowUps={}",
                newCount, String.format("%.2f", newScore), severity, maxFollowUps);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("followUpCount", newCount);
        result.put("currentScore", newScore);
        result.put("severity", severity);
        result.put("maxFollowUps", maxFollowUps);
        return result;
    }

    // ─── buildSummary ───────────────────────────────

    @Override
    public Object buildSummary(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        String severity = (String) input.get("severity");
        int followUpCount = toInt(input.get("followUpCount"));
        String visitId = input.get("visitId") != null ? input.get("visitId").toString() : "N/A";

        String summary = String.format(
                "Health Check Complete for patient %s (visit: %s). " +
                "Final severity: %s. Total follow-ups: %d.",
                patientId, visitId, severity, followUpCount);

        logger.info("[buildSummary] {}", summary);
        return summary;
    }

    // ─── helpers ────────────────────────────────────

    private static int toInt(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : 0;
    }

    private static double toDouble(Object obj) {
        return obj instanceof Number ? ((Number) obj).doubleValue() : 0.0;
    }
}
