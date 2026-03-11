package com.example.temporaldemo.engine.definition;

/**
 * Status of a workflow definition.
 *
 * <ul>
 *   <li>DRAFT    — Draft, cannot be used for starting workflows</li>
 *   <li>PUBLISHED — Published, can be used to start workflows (latest version per type is preferred)</li>
 *   <li>ARCHIVED  — Archived, cannot be used for starting workflows</li>
 * </ul>
 */
public enum DefinitionStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
