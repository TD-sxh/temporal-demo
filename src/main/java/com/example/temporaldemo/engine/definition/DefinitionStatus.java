package com.example.temporaldemo.engine.definition;

/**
 * Status of a workflow definition.
 *
 * <ul>
 *   <li>DRAFT    — 草稿，不可被启动</li>
 *   <li>PUBLISHED — 已发布，可被启动（同 type 可有多个 PUBLISHED 版本，取最新）</li>
 *   <li>ARCHIVED  — 已归档，不可被启动</li>
 * </ul>
 */
public enum DefinitionStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
