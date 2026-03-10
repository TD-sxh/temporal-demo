package com.example.temporaldemo.engine.activity;

import java.util.Map;

/**
 * Interface that all activity handlers must implement.
 *
 * <p>Each handler is registered in the {@link ActivityHandlerRegistry}
 * with a unique name. When the orchestrator encounters a TASK node,
 * the {@code activityName} is used to look up the handler and invoke it.
 *
 * <p>Example:
 * <pre>
 * public class RecordVisitHandler implements ActivityHandler {
 *     public Object handle(Map&lt;String, Object&gt; input) {
 *         String patientId = (String) input.get("patientId");
 *         // business logic ...
 *         return "VISIT-12345";
 *     }
 * }
 * </pre>
 */
@FunctionalInterface
public interface ActivityHandler {

    /**
     * Execute the activity logic.
     *
     * @param input key-value input parameters from the workflow context
     * @return the result (stored in context under the node's outputKey)
     */
    Object handle(Map<String, Object> input);
}
