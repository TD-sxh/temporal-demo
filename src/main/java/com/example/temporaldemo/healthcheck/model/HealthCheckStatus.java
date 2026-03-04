package com.example.temporaldemo.healthcheck.model;

public class HealthCheckStatus {
    private String patientId;
    private Severity currentSeverity;
    private int followUpCompleted;
    private int maxFollowUps;
    private boolean cancelled;
    private String currentPhase;

    public HealthCheckStatus() {}

    public HealthCheckStatus(String patientId, Severity currentSeverity, int followUpCompleted,
                             int maxFollowUps, boolean cancelled, String currentPhase) {
        this.patientId = patientId;
        this.currentSeverity = currentSeverity;
        this.followUpCompleted = followUpCompleted;
        this.maxFollowUps = maxFollowUps;
        this.cancelled = cancelled;
        this.currentPhase = currentPhase;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Severity getCurrentSeverity() { return currentSeverity; }
    public void setCurrentSeverity(Severity currentSeverity) { this.currentSeverity = currentSeverity; }

    public int getFollowUpCompleted() { return followUpCompleted; }
    public void setFollowUpCompleted(int followUpCompleted) { this.followUpCompleted = followUpCompleted; }

    public int getMaxFollowUps() { return maxFollowUps; }
    public void setMaxFollowUps(int maxFollowUps) { this.maxFollowUps = maxFollowUps; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    @Override
    public String toString() {
        return String.format("Patient: %s | Severity: %s | Follow-ups: %d/%d | Phase: %s | Cancelled: %s",
                patientId, currentSeverity, followUpCompleted, maxFollowUps, currentPhase, cancelled);
    }
}
