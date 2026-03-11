package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.expression.SpelEvaluator;
import com.example.temporaldemo.engine.model.NodeDefinition;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

/**
 * Executor for LOOP nodes.
 *
 * <p>Repeatedly executes the {@code loopBody} nodes as long as the
 * SpEL {@code condition} evaluates to true, up to {@code maxIterations}.
 */
public class LoopNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(LoopNodeExecutor.class);

    private final NodeExecutorRegistry executorRegistry;

    public LoopNodeExecutor(NodeExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        String condition = node.getCondition();
        int maxIterations = node.getMaxIterations();
        int iteration = 0;

        logger.info("LOOP [{}]: starting. condition='{}', maxIterations={}", node.getId(), condition, maxIterations);

        while (iteration < maxIterations) {
            // Evaluate loop condition
            boolean shouldContinue;
            try {
                shouldContinue = SpelEvaluator.evaluateBoolean(condition, context);
            } catch (Exception e) {
                String msg = "LOOP [" + node.getId() + "]: condition evaluation failed: " + e.getMessage()
                        + ". Hint: string literals in SpEL must use single quotes, e.g. #status == 'ACTIVE'";
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }

            if (!shouldContinue) {
                logger.info("LOOP [{}]: condition is false after {} iterations. Exiting.", node.getId(), iteration);
                break;
            }

            iteration++;
            logger.info("LOOP [{}]: iteration #{}", node.getId(), iteration);

            // Update iteration count in context
            context.setVariable("_loopIteration", iteration);

            // Execute all body nodes sequentially
            if (node.getLoopBody() != null) {
                for (NodeDefinition bodyNode : node.getLoopBody()) {
                    context.setCurrentNodeId(bodyNode.getId());
                    NodeExecutor executor = executorRegistry.getExecutor(bodyNode.getType());
                    String bodyNext = executor.execute(bodyNode, context);
                    // In loop body, we ignore "next" links and execute sequentially
                    // Special: if bodyNext is "__break__", break out of the loop
                    if ("__break__".equals(bodyNext)) {
                        logger.info("LOOP [{}]: break signal received at iteration {}", node.getId(), iteration);
                        return node.getNext();
                    }
                }
            }
        }

        if (iteration >= maxIterations) {
            logger.warn("LOOP [{}]: reached maxIterations ({}). Force exiting.", node.getId(), maxIterations);
        }

        return node.getNext();
    }
}
