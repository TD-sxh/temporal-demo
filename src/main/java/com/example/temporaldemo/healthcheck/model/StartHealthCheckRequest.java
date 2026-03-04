package com.example.temporaldemo.healthcheck.model;

/**
 * Request body for starting a health check workflow via HTTP.
 */
public class StartHealthCheckRequest {
    private String patientId = "P001";
    private String patientName = "John Doe";
    private String doctorName = "Dr. Smith";
    private String visitReason = "Annual physical exam";
    private String scenario; // "normal", "abnormal", "severe", or null for random
    private boolean demoMode = true;

    public StartHealthCheckRequest() {}

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
