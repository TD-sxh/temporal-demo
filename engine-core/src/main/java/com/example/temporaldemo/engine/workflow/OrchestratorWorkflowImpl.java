package com.example.temporaldemo.engine.workflow;

import com.example.temporaldemo.engine.context.WorkflowContext;
import com.example.temporaldemo.engine.executor.*;
import com.example.temporaldemo.engine.model.NodeDefinition;
import com.example.temporaldemo.engine.model.NodeType;
import com.example.temporaldemo.engine.model.WorkflowDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of the generic orchestrator workflow.
 *
 * <p>This is the "engine": it deserializes the JSON workflow definition,
 * builds the node graph, and walks through nodes from {@code startNode}
 * until there are no more transitions (next == null).
 *
 * <p>Key design:
 * <ul>
 *   <li>Uses a single {@link GenericActivity} stub for all TASK nodes</li>
 *   <li>Node executors are created at workflow start (deterministic)</li>
 *   <li>SpEL expressions evaluate against the {@link WorkflowContext}</li>
 *   <li>Signals are stored in context and consumed by WAIT nodes</li>
 * </ul>
 */
public class OrchestratorWorkflowImpl implements OrchestratorWorkflow {

    private static final Logger logger = Workflow.getLogger(OrchestratorWorkflowImpl.class);

    // Workflow context — holds all state
    private final WorkflowContext context = new WorkflowContext();

    // Untyped activity stub — no compile-time dependency on activity interfaces
    private final ActivityStub activityStub = Workflow.newUntypedActivityStub(
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .build())
                    .build());

    // Pause/resume flag — controlled via signals from workflow-admin
    private boolean paused = false;

    // Child workflow ID of the currently waiting HUMAN_TASK node (null when idle)
    private String pendingHumanTaskId = null;

    // Node executor registry
    private final NodeExecutorRegistry executorRegistry = new NodeExecutorRegistry();

    {
        // Register all node executors
        executorRegistry.register(NodeType.START, new StartEndNodeExecutor());
        executorRegistry.register(NodeType.END, new StartEndNodeExecutor());
        executorRegistry.register(NodeType.TASK, new TaskNodeExecutor(activityStub));
        executorRegistry.register(NodeType.BRANCH, new BranchNodeExecutor());
        executorRegistry.register(NodeType.WAIT, new WaitNodeExecutor());
        executorRegistry.register(NodeType.PARALLEL, new ParallelNodeExecutor(executorRegistry));
        executorRegistry.register(NodeType.LOOP, new LoopNodeExecutor(executorRegistry));
        executorRegistry.register(NodeType.DELAY, new DelayNodeExecutor());
        executorRegistry.register(NodeType.HUMAN_TASK, new HumanTaskNodeExecutor(
                id -> pendingHumanTaskId = id));
    }

    // ─── Signal ─────────────────────────────────────

    @Override
    public void signal(String signalName, Object payload) {
        logger.info("Signal received: name='{}', payload={}", signalName, payload);
        context.addSignal(signalName, payload);
    }

    @Override
    public void pause() {
        logger.info("Workflow paused.");
        paused = true;
        context.setStatusMessage("PAUSED");
    }

    @Override
    public void resume() {
        logger.info("Workflow resumed.");
        paused = false;
    }

    // ─── Query ──────────────────────────────────────

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentNodeId", context.getCurrentNodeId());
        status.put("statusMessage", context.getStatusMessage());
        status.put("paused", paused);
        if (pendingHumanTaskId != null) {
            status.put("pendingHumanTaskId", pendingHumanTaskId);
        }
        status.put("variables", context.getAllVariables());
        return status;
    }

    // ─── Main Execution ─────────────────────────────

    @Override
    public Map<String, Object> execute(String workflowDefinitionJson, Map<String, Object> inputVariables) {
        // 1. Parse the workflow definition
        WorkflowDefinition definition;
        try {
            ObjectMapper mapper = new ObjectMapper();
            definition = mapper.readValue(workflowDefinitionJson, WorkflowDefinition.class);
        } catch (Exception e) {
            logger.error("Failed to parse workflow definition: {}", e.getMessage());
            context.setStatusMessage("ERROR: " + e.getMessage());
            throw new RuntimeException("Failed to parse workflow definition: " + e.getMessage(), e);
        }

        logger.info("Orchestrator starting workflow: {} (version={})", definition.getName(), definition.getVersion());

        // 2. Initialize context with definition's initial variables + runtime input
        if (definition.getInitialVariables() != null) {
            context.putAllVariables(definition.getInitialVariables());
        }
        if (inputVariables != null) {
            context.putAllVariables(inputVariables); // runtime overrides
        }

        // 3. Build node lookup map
        Map<String, NodeDefinition> nodeMap = new HashMap<>();
        if (definition.getNodes() != null) {
            for (NodeDefinition node : definition.getNodes()) {
                nodeMap.put(node.getId(), node);
            }
        }

        // 4. Walk the node graph starting from startNode
        String currentNodeId = definition.getStartNode();
        while (currentNodeId != null) {
            NodeDefinition node = nodeMap.get(currentNodeId);
            if (node == null) {
                String msg = "Node not found: " + currentNodeId;
                logger.error(msg);
                context.setStatusMessage("ERROR: " + msg);
                throw new RuntimeException(msg);
            }

            // Block here if paused — resumes when resume() signal is received
            if (paused) {
                context.setStatusMessage("PAUSED at " + currentNodeId);
            }
            Workflow.await(() -> !paused);

            context.setCurrentNodeId(currentNodeId);
            context.setStatusMessage("EXECUTING: " + currentNodeId);
            logger.info(">>> Executing node: {} [{}]", node.getId(), node.getType());

            // Execute the node
            NodeExecutor executor = executorRegistry.getExecutor(node.getType());
            String nextNodeId = executor.execute(node, context);

            logger.info("<<< Node {} completed. Next: {}", node.getId(), nextNodeId);
            currentNodeId = nextNodeId;
        }

        // 5. Done
        context.setCurrentNodeId(null);
        context.setStatusMessage("COMPLETED");
        logger.info("Orchestrator workflow completed. Final variables: {}", context.getAllVariables());
        return context.getAllVariables();
    }
}
