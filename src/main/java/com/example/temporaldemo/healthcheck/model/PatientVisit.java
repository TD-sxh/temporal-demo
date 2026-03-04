package com.example.temporaldemo.healthcheck.model;

public class PatientVisit {
    private String patientId;
    private String patientName;
    private String doctorName;
    private String visitReason;
    private String scenario; // "normal", "abnormal", "severe" - for deterministic demo; null = random
    private boolean demoMode = true; // true: days→seconds for fast demo; false: real durations

    public PatientVisit() {}

    public PatientVisit(String patientId, String patientName, String doctorName, String visitReason) {
        this(patientId, patientName, doctorName, visitReason, null, true);
    }

    public PatientVisit(String patientId, String patientName, String doctorName, String visitReason, String scenario) {
        this(patientId, patientName, doctorName, visitReason, scenario, true);
    }

    public PatientVisit(String patientId, String patientName, String doctorName, String visitReason, String scenario, boolean demoMode) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.visitReason = visitReason;
        this.scenario = scenario;
        this.demoMode = demoMode;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getVisitReason() { return visitReason; }
    public void setVisitReason(String visitReason) { this.visitReason = visitReason; }

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }

    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }
}
