package com.example.temporaldemo.engine.model;

/**
 * Node types supported by the workflow engine.
 */
public enum NodeType {
    /** Marks the entry point of the workflow (no-op, transitions to next) */
    START,
    /** Marks a terminal point of the workflow (no-op, ends execution) */
    END,
    /** Executes a registered Activity by name */
    TASK,
    /** Conditional branching based on SpEL expression */
    BRANCH,
    /** Waits for a specific Signal before continuing */
    WAIT,
    /** Executes multiple branches in parallel and merges results */
    PARALLEL,
    /** Loops over a body of nodes based on a SpEL condition */
    LOOP,
    /** Waits for a fixed duration before continuing */
    DELAY
}
