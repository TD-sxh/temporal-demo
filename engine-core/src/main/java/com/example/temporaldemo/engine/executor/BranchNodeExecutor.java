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
                boolean result;
                try {
                    result = SpelEvaluator.evaluateBoolean(branch.getCondition(), context);
                } catch (Exception e) {
                    String msg = "BRANCH [" + node.getId() + "]: error evaluating condition '"
                            + branch.getCondition() + "': " + e.getMessage()
                            + ". Hint: string literals in SpEL must use single quotes, e.g. #severity == 'SEVERE'";
                    logger.error(msg);
                    throw new RuntimeException(msg, e);
                }
                logger.info("BRANCH [{}]: condition '{}' = {}", node.getId(), branch.getCondition(), result);
                if (result) {
                    logger.info("BRANCH [{}]: taking branch -> {}", node.getId(), branch.getNext());
                    return branch.getNext();
                }
            }
        }

        logger.info("BRANCH [{}]: no condition matched, using defaultNext -> {}", node.getId(), node.getDefaultNext());
        return node.getDefaultNext() != null ? node.getDefaultNext() : node.getNext();
    }
}
