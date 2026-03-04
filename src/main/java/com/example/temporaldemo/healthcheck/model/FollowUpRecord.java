package com.example.temporaldemo.healthcheck.model;

public class FollowUpRecord {
    private String patientId;
    private int sequenceNumber;
    private String notes;
    private String timestamp;
    private double healthScore;

    public FollowUpRecord() {}

    public FollowUpRecord(String patientId, int sequenceNumber, String notes, String timestamp, double healthScore) {
        this.patientId = patientId;
        this.sequenceNumber = sequenceNumber;
        this.notes = notes;
        this.timestamp = timestamp;
        this.healthScore = healthScore;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
}
