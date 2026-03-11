package com.example.temporaldemo.engine.definition;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistent record of a registered activity handler.
 *
 * <p>Workers upsert their activity list here on startup via
 * {@code POST /api/activities/register}, so workflow designers
 * can discover available activities from {@code GET /api/activities}.
 */
@Entity
@Table(name = "activity_catalog")
public class ActivityCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 1024)
    private String description;

    /** JSON array of expected input key names, e.g. ["patientId","visitReason"] */
    @Column(name = "input_keys_json", length = 2048)
    private String inputKeysJson;

    /** Output type hint: string, map, number, boolean */
    @Column(name = "output_type")
    private String outputType;

    /** The Temporal task queue this activity is served on */
    @Column(name = "task_queue")
    private String taskQueue;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInputKeysJson() { return inputKeysJson; }
    public void setInputKeysJson(String inputKeysJson) { this.inputKeysJson = inputKeysJson; }

    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }

    public String getTaskQueue() { return taskQueue; }
    public void setTaskQueue(String taskQueue) { this.taskQueue = taskQueue; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
