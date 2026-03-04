package com.example.temporaldemo.healthcheck.model;

public enum Severity {
    NORMAL,
    ABNORMAL,
    SEVERE;

    /**
     * Convert a health score (0.0~1.0) to a Severity level.
     * Single source of truth for score→severity mapping.
     */
    public static Severity fromScore(double score) {
        if (score < 0.4) return NORMAL;
        if (score < 0.75) return ABNORMAL;
        return SEVERE;
    }
}
