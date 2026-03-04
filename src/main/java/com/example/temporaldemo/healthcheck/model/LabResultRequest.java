package com.example.temporaldemo.healthcheck.model;

/**
 * Request body for injecting a new lab result via HTTP.
 */
public class LabResultRequest {
    private double score;

    public LabResultRequest() {}

    public LabResultRequest(double score) {
        this.score = score;
    }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
