package com.example.temporaldemo.engine.model;

import java.util.List;

/**
 * Metadata descriptor for a registered activity handler.
 *
 * <p>Workers populate this when registering handlers, then push the list
 * to the definition-service so workflow designers can discover available activities.
 */
public class ActivityInfo {

    /** Registered activity name (matches activityName in TASK node definition). */
    private String name;

    /** Human-readable description of what this activity does. */
    private String description;

    /**
     * Expected input parameter keys.
     * Corresponds to the keys the handler reads from the input Map.
     */
    private List<String> inputKeys;

    /**
     * Output type hint: "string", "map", "number", "boolean".
     * "map" means the result is a Map and outputKey can extract individual fields.
     */
    private String outputType;

    /** Temporal task queue this activity is registered on. */
    private String taskQueue;

    public ActivityInfo() {}

    public static ActivityInfo of(String description, List<String> inputKeys, String outputType) {
        ActivityInfo info = new ActivityInfo();
        info.description = description;
        info.inputKeys = inputKeys;
        info.outputType = outputType;
        return info;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getInputKeys() { return inputKeys; }
    public void setInputKeys(List<String> inputKeys) { this.inputKeys = inputKeys; }

    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }

    public String getTaskQueue() { return taskQueue; }
    public void setTaskQueue(String taskQueue) { this.taskQueue = taskQueue; }
}
