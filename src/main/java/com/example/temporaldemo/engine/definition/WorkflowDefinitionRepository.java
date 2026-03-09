package com.example.temporaldemo.engine.definition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, Long> {

    /**
     * Find the latest PUBLISHED version for a given type.
     * Used at workflow start time.
     */
    Optional<WorkflowDefinitionEntity> findFirstByTypeAndStatusOrderByVersionDesc(
            String type, DefinitionStatus status);

    /**
     * Find a specific version of a type.
     */
    Optional<WorkflowDefinitionEntity> findByTypeAndVersion(String type, Integer version);

    /**
     * List all versions of a given type (newest first).
     */
    List<WorkflowDefinitionEntity> findByTypeOrderByVersionDesc(String type);

    /**
     * List all distinct types (for "list all workflow types" API).
     */
    @Query("SELECT DISTINCT d.type FROM WorkflowDefinitionEntity d ORDER BY d.type")
    List<String> findAllTypes();

    /**
     * Find the max version number for a given type (for auto-increment).
     */
    @Query("SELECT COALESCE(MAX(d.version), 0) FROM WorkflowDefinitionEntity d WHERE d.type = :type")
    int findMaxVersionByType(@Param("type") String type);

    /**
     * List latest version of each type (for overview).
     */
    @Query("""
            SELECT d FROM WorkflowDefinitionEntity d
            WHERE d.version = (
                SELECT MAX(d2.version) FROM WorkflowDefinitionEntity d2 WHERE d2.type = d.type
            )
            ORDER BY d.type
            """)
    List<WorkflowDefinitionEntity> findLatestVersionPerType();
}
