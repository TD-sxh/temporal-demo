package com.example.temporaldemo.engine.definition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityCatalogRepository extends JpaRepository<ActivityCatalogEntity, Long> {

    Optional<ActivityCatalogEntity> findByName(String name);
}
