package com.example.temporaldemo.engine.definition;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for workflow definition management.
 *
 * <p>Lookup strategy:
 * <ol>
 *   <li>DB 查询（优先取 PUBLISHED 的最新版本）</li>
 *   <li>如 DB 查不到，回退到 classpath 下 {@code workflows/<type>.json}</li>
 * </ol>
 */
@Service
public class WorkflowDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionService.class);
    private static final String CLASSPATH_PREFIX = "workflows/";

    private final WorkflowDefinitionRepository repository;
    private final ObjectMapper objectMapper;

    public WorkflowDefinitionService(WorkflowDefinitionRepository repository,
                                     ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // ─── Resolve (for starting workflows) ────────────────────────

    /**
     * Resolve the definition JSON for a given type, using the latest PUBLISHED version.
     * Falls back to classpath if not found in DB.
     *
     * @return a {@link ResolvedDefinition} containing the JSON and version info
     * @throws DefinitionNotFoundException if not found anywhere
     */
    public ResolvedDefinition resolve(String type) {
        // 1. Try DB: latest PUBLISHED version
        Optional<WorkflowDefinitionEntity> dbResult = repository
                .findFirstByTypeAndStatusOrderByVersionDesc(type, DefinitionStatus.PUBLISHED);

        if (dbResult.isPresent()) {
            WorkflowDefinitionEntity entity = dbResult.get();
            logger.info("Resolved definition '{}' from DB: version={}", type, entity.getVersion());
            return new ResolvedDefinition(
                    entity.getDefinitionJson(),
                    entity.getVersion(),
                    "DATABASE",
                    entity.getId());
        }

        // 2. Fallback: classpath
        String classpathJson = loadFromClasspath(type);
        if (classpathJson != null) {
            logger.info("Resolved definition '{}' from classpath fallback", type);
            return new ResolvedDefinition(classpathJson, 0, "CLASSPATH", null);
        }

        throw new DefinitionNotFoundException(
                "Workflow definition not found for type: '" + type +
                "'. Neither in DB (PUBLISHED) nor at classpath: " + CLASSPATH_PREFIX + type + ".json");
    }

    /**
     * Resolve a specific version of a type.
     */
    public ResolvedDefinition resolve(String type, int version) {
        WorkflowDefinitionEntity entity = repository.findByTypeAndVersion(type, version)
                .orElseThrow(() -> new DefinitionNotFoundException(
                        "Definition not found: type='" + type + "', version=" + version));
        return new ResolvedDefinition(
                entity.getDefinitionJson(),
                entity.getVersion(),
                "DATABASE",
                entity.getId());
    }

    // ─── CRUD ────────────────────────────────────────────────────

    /**
     * Create a new version (DRAFT) for a given type.
     * Version number is auto-incremented.
     */
    @Transactional
    public WorkflowDefinitionEntity create(String name,
                                           String definitionJson, String description) {
        // Validate JSON and extract type from JSON "id" field
        validateJson(definitionJson);
        String type = extractTypeFromJson(definitionJson);

        int nextVersion = repository.findMaxVersionByType(type) + 1;

        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setType(type);
        entity.setName(name);
        entity.setVersion(nextVersion);
        entity.setDefinitionJson(definitionJson);
        entity.setStatus(DefinitionStatus.DRAFT);
        entity.setDescription(description);

        entity = repository.save(entity);
        logger.info("Created definition: type={}, version={}, id={}", type, nextVersion, entity.getId());
        return entity;
    }

    /**
     * Update the definition JSON of an existing DRAFT definition.
     */
    @Transactional
    public WorkflowDefinitionEntity update(Long id, String definitionJson, String name, String description) {
        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException("Definition not found: id=" + id));

        if (entity.getStatus() != DefinitionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT definitions can be updated. Current status: " + entity.getStatus());
        }

        if (definitionJson != null) {
            validateJson(definitionJson);
            String jsonType = extractTypeFromJson(definitionJson);
            if (!entity.getType().equals(jsonType)) {
                throw new IllegalArgumentException(
                        "JSON 'id' field ('" + jsonType + "') does not match the definition type ('" + entity.getType() + "'). Cannot change type via update.");
            }
            entity.setDefinitionJson(definitionJson);
        }
        if (name != null) {
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }

        entity = repository.save(entity);
        logger.info("Updated definition: id={}, type={}, version={}", id, entity.getType(), entity.getVersion());
        return entity;
    }

    /**
     * Publish a DRAFT definition so it becomes available for startup.
     */
    @Transactional
    public WorkflowDefinitionEntity publish(Long id) {
        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException("Definition not found: id=" + id));

        if (entity.getStatus() != DefinitionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT definitions can be published. Current status: " + entity.getStatus());
        }

        entity.setStatus(DefinitionStatus.PUBLISHED);
        entity = repository.save(entity);
        logger.info("Published definition: id={}, type={}, version={}", id, entity.getType(), entity.getVersion());
        return entity;
    }

    /**
     * Archive a PUBLISHED definition (soft-delete, won't be used for new startups).
     */
    @Transactional
    public WorkflowDefinitionEntity archive(Long id) {
        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException("Definition not found: id=" + id));

        entity.setStatus(DefinitionStatus.ARCHIVED);
        entity = repository.save(entity);
        logger.info("Archived definition: id={}, type={}, version={}", id, entity.getType(), entity.getVersion());
        return entity;
    }

    /**
     * Delete a DRAFT definition permanently.
     */
    @Transactional
    public void delete(Long id) {
        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException("Definition not found: id=" + id));

        if (entity.getStatus() != DefinitionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT definitions can be deleted. Current status: " + entity.getStatus() +
                    ". Use archive instead.");
        }

        repository.delete(entity);
        logger.info("Deleted definition: id={}, type={}, version={}", id, entity.getType(), entity.getVersion());
    }

    // ─── Query ───────────────────────────────────────────────────

    public Optional<WorkflowDefinitionEntity> findById(Long id) {
        return repository.findById(id);
    }

    public List<WorkflowDefinitionEntity> listByType(String type) {
        return repository.findByTypeOrderByVersionDesc(type);
    }

    public List<WorkflowDefinitionEntity> listAllLatest() {
        return repository.findLatestVersionPerType();
    }

    public List<String> listAllTypes() {
        return repository.findAllTypes();
    }

    // ─── Internal ────────────────────────────────────────────────

    private String loadFromClasspath(String type) {
        String path = CLASSPATH_PREFIX + type + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.warn("Failed to load classpath resource: {}", path, e);
            return null;
        }
    }

    private void validateJson(String json) {
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Extract the "id" field from definitionJson as the workflow type.
     * @throws IllegalArgumentException if "id" is missing or blank
     */
    public String extractTypeFromJson(String definitionJson) {
        try {
            var tree = objectMapper.readTree(definitionJson);
            var idNode = tree.get("id");
            if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "definitionJson must contain a non-empty 'id' field as the workflow type identifier");
            }
            return idNode.asText();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse definitionJson: " + e.getMessage(), e);
        }
    }

    // ─── Inner types ─────────────────────────────────────────────

    /**
     * Result of resolving a workflow definition.
     */
    public record ResolvedDefinition(
            String definitionJson,
            int version,
            String source,      // "DATABASE" or "CLASSPATH"
            Long entityId       // null if from classpath
    ) {}
}
