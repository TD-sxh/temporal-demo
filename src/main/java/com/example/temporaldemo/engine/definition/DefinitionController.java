package com.example.temporaldemo.engine.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD REST controller for managing workflow definitions.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST   /api/definitions                — Create a new definition version (DRAFT)</li>
 *   <li>GET    /api/definitions                — List all types (latest version per type)</li>
 *   <li>GET    /api/definitions/types           — List all distinct type names</li>
 *   <li>GET    /api/definitions/{type}          — List all versions of a type</li>
 *   <li>GET    /api/definitions/{type}/{version}— Get specific version</li>
 *   <li>PUT    /api/definitions/{id}            — Update a DRAFT definition</li>
 *   <li>POST   /api/definitions/{id}/publish    — Publish a DRAFT → PUBLISHED</li>
 *   <li>POST   /api/definitions/{id}/archive    — Archive (soft-delete)</li>
 *   <li>DELETE  /api/definitions/{id}            — Hard-delete a DRAFT</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/definitions")
public class DefinitionController {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionController.class);

    private final WorkflowDefinitionService service;

    public DefinitionController(WorkflowDefinitionService service) {
        this.service = service;
    }

    // ─── Create ──────────────────────────────────────────────────

    /**
     * Create a new version of a workflow definition.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "type": "health-check-flow",
     *   "name": "Health Check Workflow",
     *   "definitionJson": "{ ... full JSON ... }",
     *   "description": "Initial version"
     * }
     * </pre>
     *
     * <p>The {@code definitionJson} field can be a raw JSON string or
     * an embedded JSON object (will be serialized).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        String name = (String) request.get("name");
        String description = (String) request.get("description");

        // definitionJson can be a String or an embedded object
        Object defObj = request.get("definitionJson");
        String definitionJson;
        if (defObj instanceof String str) {
            definitionJson = str;
        } else {
            try {
                definitionJson = new tools.jackson.databind.ObjectMapper()
                        .writeValueAsString(defObj);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid definitionJson: " + e.getMessage()));
            }
        }

        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "type is required"));
        }
        if (definitionJson == null || definitionJson.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "definitionJson is required"));
        }

        WorkflowDefinitionEntity entity = service.create(type, name, definitionJson, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(entity));
    }

    // ─── Read ────────────────────────────────────────────────────

    /**
     * List all workflow types with their latest version info.
     */
    @GetMapping
    public List<Map<String, Object>> listAll() {
        return service.listAllLatest().stream().map(this::toMap).toList();
    }

    /**
     * List all distinct type names.
     */
    @GetMapping("/types")
    public List<String> listTypes() {
        return service.listAllTypes();
    }

    /**
     * List all versions of a given type.
     */
    @GetMapping("/{type}")
    public List<Map<String, Object>> listVersions(@PathVariable String type) {
        return service.listByType(type).stream().map(this::toMap).toList();
    }

    /**
     * Get a specific version of a type.
     */
    @GetMapping("/{type}/{version}")
    public ResponseEntity<Map<String, Object>> getVersion(
            @PathVariable String type, @PathVariable Integer version) {
        return service.findById(
                service.resolve(type, version).entityId()
        ).map(e -> ResponseEntity.ok(toDetailMap(e)))
         .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get definition by database ID.
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(e -> ResponseEntity.ok(toDetailMap(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Update ──────────────────────────────────────────────────

    /**
     * Update a DRAFT definition.
     *
     * <p>Request body (all fields optional):
     * <pre>
     * {
     *   "name": "Updated Name",
     *   "definitionJson": "{ ... }",
     *   "description": "Updated desc"
     * }
     * </pre>
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");

        Object defObj = request.get("definitionJson");
        String definitionJson = null;
        if (defObj instanceof String str) {
            definitionJson = str;
        } else if (defObj != null) {
            try {
                definitionJson = new tools.jackson.databind.ObjectMapper()
                        .writeValueAsString(defObj);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid definitionJson: " + e.getMessage()));
            }
        }

        WorkflowDefinitionEntity entity = service.update(id, definitionJson, name, description);
        return ResponseEntity.ok(toMap(entity));
    }

    // ─── Status transitions ──────────────────────────────────────

    /**
     * Publish a DRAFT definition.
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publish(@PathVariable Long id) {
        WorkflowDefinitionEntity entity = service.publish(id);
        return ResponseEntity.ok(toMap(entity));
    }

    /**
     * Archive a definition.
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<Map<String, Object>> archive(@PathVariable Long id) {
        WorkflowDefinitionEntity entity = service.archive(id);
        return ResponseEntity.ok(toMap(entity));
    }

    // ─── Delete ──────────────────────────────────────────────────

    /**
     * Delete a DRAFT definition permanently.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully", "id", id.toString()));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private Map<String, Object> toMap(WorkflowDefinitionEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("type", entity.getType());
        map.put("name", entity.getName());
        map.put("version", entity.getVersion());
        map.put("status", entity.getStatus());
        map.put("description", entity.getDescription());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toDetailMap(WorkflowDefinitionEntity entity) {
        Map<String, Object> map = toMap(entity);
        map.put("definitionJson", entity.getDefinitionJson());
        return map;
    }
}
