package com.example.temporaldemo.definitionservice;

import com.example.temporaldemo.engine.definition.DefinitionStatus;
import com.example.temporaldemo.engine.definition.WorkflowDefinitionEntity;
import com.example.temporaldemo.engine.definition.WorkflowDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads workflow JSON files from classpath {@code workflows/*.json} into DB
 * on startup. Skips types that already have at least one version in DB.
 * Inserted definitions are set to PUBLISHED so they are immediately usable.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final WorkflowDefinitionRepository repository;
    private final ObjectMapper objectMapper;

    public DataInitializer(WorkflowDefinitionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:workflows/*.json");

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) continue;

            try (InputStream is = resource.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(json);

                String type = root.has("id") ? root.get("id").asText() : filename.replace(".json", "");
                String name = root.has("name") ? root.get("name").asText() : type;

                // Skip if this type already exists in DB
                if (repository.findMaxVersionByType(type) > 0) {
                    logger.info("Definition '{}' already exists in DB, skipping initialization", type);
                    continue;
                }

                WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
                entity.setType(type);
                entity.setName(name);
                entity.setVersion(1);
                entity.setDefinitionJson(json);
                entity.setStatus(DefinitionStatus.PUBLISHED);
                entity.setDescription("Auto-initialized from classpath");

                repository.save(entity);
                logger.info("Initialized definition into DB: type={}, name={}, version=1, status=PUBLISHED", type, name);
            } catch (Exception e) {
                logger.warn("Failed to initialize definition from {}: {}", filename, e.getMessage());
            }
        }
    }
}
