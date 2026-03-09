package com.example.temporaldemo.engine.definition;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for workflow definitions stored in database.
 *
 * <p>Table: {@code workflow_definition}
 *
 * <p>Supports multi-version per type. The combination (type, version) is unique.
 * Status lifecycle: DRAFT → PUBLISHED → ARCHIVED.
 * When starting a workflow, the latest PUBLISHED version is used.
 */
@Entity
@Table(name = "workflow_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"type", "version"}))
public class WorkflowDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Workflow type identifier, e.g. "health-check-flow" */
    @Column(nullable = false, length = 128)
    private String type;

    /** Human-readable name */
    @Column(length = 256)
    private String name;

    /** Version number within this type (auto-incremented per type) */
    @Column(nullable = false)
    private Integer version;

    /** Full JSON definition */
    @Column(name = "definition_json", nullable = false, columnDefinition = "TEXT")
    private String definitionJson;

    /** Status: DRAFT / PUBLISHED / ARCHIVED */
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DefinitionStatus status = DefinitionStatus.DRAFT;

    /** Optional description */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getDefinitionJson() { return definitionJson; }
    public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }

    public DefinitionStatus getStatus() { return status; }
    public void setStatus(DefinitionStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
