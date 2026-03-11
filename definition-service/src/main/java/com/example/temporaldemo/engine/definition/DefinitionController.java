package com.example.temporaldemo.engine.definition;

import com.example.temporaldemo.engine.definition.WorkflowDefinitionService.ResolvedDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing workflow definitions.
 *
 * <p>CRUD endpoints + resolve endpoints for engine-api consumption.
 */
@RestController
@RequestMapping("/api/definitions")
public class DefinitionController {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionController.class);

    private final WorkflowDefinitionService service;

    public DefinitionController(WorkflowDefinitionService service) {
        this.service = service;
    }

    // ─── Resolve (used by engine-api) ────────────────────────────

    /**
     * Resolve the latest PUBLISHED definition for a type.
     * Falls back to classpath if not found in DB.
     */
    @GetMapping("/resolve/{type}")
    public ResponseEntity<Map<String, Object>> resolve(@PathVariable String type) {
        ResolvedDefinition resolved = service.resolve(type);
        return ResponseEntity.ok(toResolvedMap(resolved));
    }

    /**
     * Resolve a specific version of a type.
     */
    @GetMapping("/resolve/{type}/{version}")
    public ResponseEntity<Map<String, Object>> resolveVersion(
            @PathVariable String type, @PathVariable int version) {
        ResolvedDefinition resolved = service.resolve(type, version);
        return ResponseEntity.ok(toResolvedMap(resolved));
    }

    // ─── Create ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        String name = (String) request.get("name");
        String description = (String) request.get("description");

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

    @GetMapping
    public List<Map<String, Object>> listAll() {
        return service.listAllLatest().stream().map(this::toMap).toList();
    }

    @GetMapping("/types")
    public List<String> listTypes() {
        return service.listAllTypes();
    }

    @GetMapping("/{type}")
    public List<Map<String, Object>> listVersions(@PathVariable String type) {
        return service.listByType(type).stream().map(this::toMap).toList();
    }

    @GetMapping("/{type}/{version}")
    public ResponseEntity<Map<String, Object>> getVersion(
            @PathVariable String type, @PathVariable Integer version) {
        return service.findById(
                service.resolve(type, version).entityId()
        ).map(e -> ResponseEntity.ok(toDetailMap(e)))
         .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(e -> ResponseEntity.ok(toDetailMap(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Update ──────────────────────────────────────────────────

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

    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publish(@PathVariable Long id) {
        WorkflowDefinitionEntity entity = service.publish(id);
        return ResponseEntity.ok(toMap(entity));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Map<String, Object>> archive(@PathVariable Long id) {
        WorkflowDefinitionEntity entity = service.archive(id);
        return ResponseEntity.ok(toMap(entity));
    }

    // ─── Delete ──────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully", "id", id.toString()));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private Map<String, Object> toResolvedMap(ResolvedDefinition resolved) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("definitionJson", resolved.definitionJson());
        map.put("version", resolved.version());
        map.put("source", resolved.source());
        map.put("entityId", resolved.entityId());
        return map;
    }

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
