package com.example.temporaldemo.engine.executor;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.model.NodeDefinition;
import com.example.temporaldemo.engine.model.ParallelBranch;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executor for PARALLEL nodes.
 *
 * <p>Executes all parallel branches concurrently using {@link Async#function}.
 * Each branch runs its internal nodes sequentially.  All branches must complete
 * before the workflow moves to the next node.
 *
 * <p>Each branch's outputs are merged into the main workflow context. If
 * two branches write the same key, the last one wins (non-deterministic order).
 */
public class ParallelNodeExecutor implements NodeExecutor {

    private static final Logger logger = Workflow.getLogger(ParallelNodeExecutor.class);

    private final NodeExecutorRegistry executorRegistry;

    public ParallelNodeExecutor(NodeExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @Override
    public String execute(NodeDefinition node, WorkflowContext context) {
        List<ParallelBranch> branches = node.getParallelBranches();
        if (branches == null || branches.isEmpty()) {
            logger.warn("PARALLEL [{}]: no branches defined, skipping", node.getId());
            return node.getNext();
        }

        logger.info("PARALLEL [{}]: launching {} branches", node.getId(), branches.size());

        // Launch each branch as an async promise
        List<Promise<WorkflowContext>> promises = new ArrayList<>();
        for (ParallelBranch branch : branches) {
            Promise<WorkflowContext> promise = Async.function(() -> executeBranch(branch, context));
            promises.add(promise);
        }

        // Wait for all branches to complete
        Promise.allOf(promises).get();

        // Merge results from all branches back into main context
        for (int i = 0; i < promises.size(); i++) {
            WorkflowContext branchCtx = promises.get(i).get();
            String branchId = branches.get(i).getBranchId();
            logger.info("PARALLEL [{}]: merging results from branch '{}'", node.getId(), branchId);

            // Merge outputs into main context with branch prefix
            for (Map.Entry<String, Object> entry : branchCtx.getAllNodeOutputs().entrySet()) {
                context.setNodeOutput(entry.getKey(), entry.getValue());
            }
            // Also merge any variables set during the branch
            context.putAllVariables(branchCtx.getAllVariables());
        }

        logger.info("PARALLEL [{}]: all branches completed", node.getId());
        return node.getNext();
    }

    /**
     * Execute a single parallel branch: run its nodes sequentially
     * in a cloned context.
     */
    private WorkflowContext executeBranch(ParallelBranch branch, WorkflowContext parentContext) {
        logger.info("PARALLEL branch '{}': starting with {} nodes", branch.getBranchId(),
                branch.getNodes() != null ? branch.getNodes().size() : 0);

        // Create a branch context that inherits parent variables
        WorkflowContext branchCtx = new WorkflowContext();
        branchCtx.putAllVariables(parentContext.getAllVariables());

        if (branch.getNodes() != null) {
            for (NodeDefinition branchNode : branch.getNodes()) {
                branchCtx.setCurrentNodeId(branchNode.getId());
                NodeExecutor executor = executorRegistry.getExecutor(branchNode.getType());
                executor.execute(branchNode, branchCtx);
                // In parallel branches, we don't follow "next" links —
                // we execute nodes sequentially as listed
            }
        }

        logger.info("PARALLEL branch '{}': completed", branch.getBranchId());
        return branchCtx;
    }
}
