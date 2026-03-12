package com.example.temporaldemo.engine.model;

/**
 * Structured timeout configuration.
 *
 * <p>JSON example:
 * <pre>{ "value": 2, "unit": "minutes" }</pre>
 *
 * <p>Supported units (case-insensitive): seconds, minutes, hours, days.
 */
public class TimeoutConfig {

    private int value;

    /** Time unit: seconds, minutes, hours, days. */
    private String unit;

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    /** Converts this timeout to seconds. Returns 0 if value is unset. */
    public int toSeconds() {
        if (value <= 0) return 0;
        if (unit == null) return value;
        return switch (unit.toLowerCase()) {
            case "minutes", "minute" -> value * 60;
            case "hours", "hour"     -> value * 3600;
            case "days", "day"       -> value * 86400;
            default                  -> value; // seconds
        };
    }
}
