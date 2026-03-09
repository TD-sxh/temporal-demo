package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.activity.GenericActivity;
import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.expression.SpelEvaluator;
import com.example.temporaldemo.engine.model.NodeDefinition;
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

    private final GenericActivity activityStub;

    public TaskNodeExecutor(GenericActivity activityStub) {
        this.activityStub = activityStub;
    }

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        String activityName = node.getActivityName();
        logger.info("TASK [{}]: executing activity '{}'", node.getId(), activityName);

        // Resolve SpEL expressions in input map
        Map<String, Object> resolvedInput = SpelEvaluator.resolveInputs(node.getInput(), context);
        logger.info("TASK [{}]: resolved input keys: {}", node.getId(), resolvedInput.keySet());

        // Call the generic activity
        Object result = activityStub.execute(activityName, resolvedInput);

        // Store result in context — supports String or Map<String,String> outputKey
        storeOutput(node, context, result);

        return node.getNext();
    }

    /**
     * Store activity result into workflow context.
     * <ul>
     *   <li>String outputKey: store whole result under that key; explode if result is a Map</li>
     *   <li>Map outputKey: selective field mapping {contextVar: resultField}, "*" = whole result</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    static void storeOutput(NodeDefinition node, WorkflowContext context, Object result) {
        Object outputKey = node.getOutputKey();
        if (outputKey == null) return;

        if (outputKey instanceof String) {
            String key = (String) outputKey;
            if (key.isEmpty()) return;
            context.setNodeOutput(key, result);

            // Explode Map result entries as individual variables
            if (result instanceof Map) {
                Map<String, Object> mapResult = (Map<String, Object>) result;
                for (Map.Entry<String, Object> entry : mapResult.entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }
        } else if (outputKey instanceof Map) {
            Map<String, String> mapping = (Map<String, String>) outputKey;
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String contextKey = entry.getKey();
                String sourceField = entry.getValue();

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
}
