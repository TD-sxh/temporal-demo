package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.expression.SpelEvaluator;
import com.example.temporaldemo.engine.model.BranchCase;
import com.example.temporaldemo.engine.model.NodeDefinition;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.util.List;

/**
 * Executor for BRANCH nodes.
 *
 * <p>Evaluates a list of SpEL conditions in order. The first condition
 * that evaluates to {@code true} determines the next node. Falls back
 * to {@code defaultNext} if no condition matches.
 */
public class BranchNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(BranchNodeExecutor.class);

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        logger.info("BRANCH [{}]: evaluating conditions", node.getId());

        List<BranchCase> branches = node.getBranches();
        if (branches != null) {
            for (BranchCase branch : branches) {
                try {
                    boolean result = SpelEvaluator.evaluateBoolean(branch.getCondition(), context);
                    logger.info("BRANCH [{}]: condition '{}' = {}", node.getId(), branch.getCondition(), result);
                    if (result) {
                        logger.info("BRANCH [{}]: taking branch -> {}", node.getId(), branch.getNext());
                        return branch.getNext();
                    }
                } catch (Exception e) {
                    logger.warn("BRANCH [{}]: error evaluating '{}': {}",
                            node.getId(), branch.getCondition(), e.getMessage());
                }
            }
        }

        logger.info("BRANCH [{}]: no condition matched, using defaultNext -> {}", node.getId(), node.getDefaultNext());
        return node.getDefaultNext() != null ? node.getDefaultNext() : node.getNext();
    }
}
