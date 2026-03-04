package com.example.temporaldemo.healthcheck.model;

public class DiagnosisResult {
    private String patientId;
    private String diagnosisCode;
    private String diagnosisDescription;
    private double score;

    public DiagnosisResult() {}

    public DiagnosisResult(String patientId, String diagnosisCode, String diagnosisDescription, double score) {
        this.patientId = patientId;
        this.diagnosisCode = diagnosisCode;
        this.diagnosisDescription = diagnosisDescription;
        this.score = score;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(String diagnosisCode) { this.diagnosisCode = diagnosisCode; }

    public String getDiagnosisDescription() { return diagnosisDescription; }
    public void setDiagnosisDescription(String diagnosisDescription) { this.diagnosisDescription = diagnosisDescription; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
