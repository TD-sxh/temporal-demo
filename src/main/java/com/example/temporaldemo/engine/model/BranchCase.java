package com.example.temporaldemo.engine.model;

/**
 * A single branch in a BRANCH node.
 *
 * <p>The {@code condition} is a SpEL expression evaluated against the
 * {@link WorkflowContext} variables. If it evaluates to {@code true},
 * the workflow transitions to {@code next}.
 *
 * <p>JSON example:
 * <pre>
 * { "condition": "#severity == 'SEVERE'", "next": "notify_doctor" }
 * </pre>
 */
public class BranchCase {

    /** SpEL expression that should evaluate to boolean */
    private String condition;

    /** Node ID to jump to when condition is true */
    private String next;

    public BranchCase() {}

    public BranchCase(String condition, String next) {
        this.condition = condition;
        this.next = next;
    }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }
}
