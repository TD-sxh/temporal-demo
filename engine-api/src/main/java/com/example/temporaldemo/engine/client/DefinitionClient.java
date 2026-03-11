package com.example.temporaldemo.engine.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client for calling the definition-service resolve endpoints.
 */
@Component
public class DefinitionClient {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionClient.class);

    private final RestClient restClient;

    public DefinitionClient(@Value("${definition.service.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Resolve the latest PUBLISHED definition for a type.
     */
    public ResolvedDefinition resolve(String type) {
        logger.debug("Resolving definition for type '{}' from definition-service", type);
        Map<String, Object> body = restClient.get()
                .uri("/api/definitions/resolve/{type}", type)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    throw new RuntimeException(
                            "Failed to resolve definition for type '" + type +
                            "': HTTP " + resp.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});
        return mapToResolved(body);
    }

    /**
     * Resolve a specific version of a type.
     */
    public ResolvedDefinition resolve(String type, int version) {
        logger.debug("Resolving definition for type '{}' version {} from definition-service", type, version);
        Map<String, Object> body = restClient.get()
                .uri("/api/definitions/resolve/{type}/{version}", type, version)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    throw new RuntimeException(
                            "Failed to resolve definition for type '" + type +
                            "' version " + version + ": HTTP " + resp.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});
        return mapToResolved(body);
    }

    private ResolvedDefinition mapToResolved(Map<String, Object> body) {
        return new ResolvedDefinition(
                (String) body.get("definitionJson"),
                body.get("version") instanceof Number n ? n.intValue() : 0,
                (String) body.get("source"),
                body.get("entityId") instanceof Number n ? n.longValue() : null
        );
    }

    /**
     * Resolved definition data from the definition-service.
     */
    public record ResolvedDefinition(
            String definitionJson,
            int version,
            String source,
            Long entityId
    ) {}
}
