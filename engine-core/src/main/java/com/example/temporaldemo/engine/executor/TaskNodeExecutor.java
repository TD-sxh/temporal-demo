package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.expression.SpelEvaluator;
import com.example.temporaldemo.engine.model.NodeDefinition;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Executor for TASK nodes.
 *
 * <p>Invokes a registered activity via the {@link GenericActivity} stub,
 * resolves SpEL expressions in input parameters, and stores the result
 * in the workflow context.
 */
public class TaskNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(TaskNodeExecutor.class);

    private final ActivityStub activityStub;

    public TaskNodeExecutor(ActivityStub activityStub) {
        this.activityStub = activityStub;
    }

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        // Temporal registers activity methods with PascalCase (e.g. "RecordVisit").
        // JSON definitions use camelCase (e.g. "recordVisit"). Capitalize first letter to match.
        String rawName = node.getActivityName();
        String activityName = rawName == null || rawName.isEmpty()
                ? rawName
                : Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1);
        logger.info("TASK [{}]: executing activity '{}'", node.getId(), activityName);

        // Resolve SpEL expressions in input map
        Map<String, Object> resolvedInput = SpelEvaluator.resolveInputs(node.getInput(), context);
        logger.info("TASK [{}]: resolved input keys: {}", node.getId(), resolvedInput.keySet());

        // Call the activity by name (untyped) — maps to @ActivityMethod in the worker
        Object result = activityStub.execute(activityName, Object.class, resolvedInput);

        // Store result in context — supports String or Map<String,String> outputKey
        storeOutput(node, context, result);

        return node.getNext();
    }

    /**
     * Store activity result into workflow context using the outputKey mapping.
     *
     * <p>outputKey is a Map where:
     * <ul>
     *   <li>Key with '#' prefix (e.g. "#visitId") → strip '#', store as global context variable "visitId"</li>
     *   <li>Key without '#' prefix → store as-is in context</li>
     *   <li>Value "*" → store the entire activity result</li>
     *   <li>Other value (e.g. "status") → extract that field from the result Map</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    static void storeOutput(NodeDefinition node, WorkflowContext context, Object result) {
        Map<String, String> outputKey = node.getOutputKey();
        if (outputKey == null || outputKey.isEmpty()) return;

        for (Map.Entry<String, String> entry : outputKey.entrySet()) {
            String rawKey = entry.getKey();
            String sourceField = entry.getValue();

            // Strip '#' prefix to get the actual variable name
            String contextKey = rawKey.startsWith("#") ? rawKey.substring(1) : rawKey;

            if ("*".equals(sourceField)) {
                // Store the entire result
                context.setNodeOutput(contextKey, result);
            } else if (result instanceof Map) {
                // Extract specific field from result Map
                Map<String, Object> resultMap = (Map<String, Object>) result;
                context.setNodeOutput(contextKey, resultMap.get(sourceField));
            }
        }
    }
}
