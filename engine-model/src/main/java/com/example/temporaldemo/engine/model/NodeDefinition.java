package com.example.temporaldemo.engine.model;

import java.util.List;
import java.util.Map;

/**
 * Definition of a single node in a workflow graph.
 *
 * <p>JSON example (TASK node):
 * <pre>
 * {
 *   "id": "record_visit",
 *   "name": "Record Visit",
 *   "type": "TASK",
 *   "activityName": "recordVisit",
 *   "input": { "patientId": "${context.patientId}" },
 *   "outputKey": "visitId",
 *   "next": "get_diagnosis"
 * }
 * </pre>
 *
 * <p>JSON example (BRANCH node):
 * <pre>
 * {
 *   "id": "check_severity",
 *   "type": "BRANCH",
 *   "branches": [
 *     { "condition": "#severity == 'SEVERE'", "next": "notify_doctor" },
 *     { "condition": "#severity == 'ABNORMAL'", "next": "follow_up" }
 *   ],
 *   "defaultNext": "build_summary"
 * }
 * </pre>
 *
 * <p>JSON example (WAIT node):
 * <pre>
 * {
 *   "id": "wait_lab_result",
 *   "type": "WAIT",
 *   "signalName": "labResult",
 *   "outputKey": "labScore",
 *   "timeoutSeconds": 300,
 *   "timeoutNext": "build_summary",
 *   "next": "analyze_lab"
 * }
 * </pre>
 *
 * <p>JSON example (PARALLEL node):
 * <pre>
 * {
 *   "id": "parallel_checks",
 *   "type": "PARALLEL",
 *   "parallelBranches": [
 *     { "branchId": "blood", "nodes": [...] },
 *     { "branchId": "xray",  "nodes": [...] }
 *   ],
 *   "next": "merge_results"
 * }
 * </pre>
 *
 * <p>JSON example (LOOP node):
 * <pre>
 * {
 *   "id": "follow_up_loop",
 *   "type": "LOOP",
 *   "condition": "#followUpCount < #maxFollowUps",
 *   "maxIterations": 20,
 *   "loopBody": [...],
 *   "next": "build_summary"
 * }
 * </pre>
 *
 * <p>JSON example (DELAY node):
 * <pre>
 * {
 *   "id": "wait_30s",
 *   "type": "DELAY",
 *   "delaySeconds": 30,
 *   "next": "next_step"
 * }
 * </pre>
 */
public class NodeDefinition {

    private String id;
    private String name;
    private NodeType type;

    // --- TASK ---
    /** Activity name in the registry */
    private String activityName;
    /** Input parameters (values may contain SpEL like "${context.patientId}") */
    private Map<String, Object> input;
    /** Key to store the activity result in workflow context.
     *  Can be a String (single key) or a Map&lt;String,String&gt; (field mapping).
     *  Map form: {"contextVar": "resultField"}, special value "*" means the whole result. */
    private Object outputKey;

    // --- BRANCH ---
    /** List of conditional branches (evaluated in order) */
    private List<BranchCase> branches;
    /** Fallback next node if no branch condition matches */
    private String defaultNext;

    // --- WAIT ---
    /** Signal name to wait for */
    private String signalName;
    /** Timeout in seconds (optional, 0 = no timeout) */
    private int timeoutSeconds;
    /** Node to go to if timeout occurs */
    private String timeoutNext;

    // --- PARALLEL ---
    /** Parallel branches to execute concurrently */
    private List<ParallelBranch> parallelBranches;

    // --- LOOP ---
    /** SpEL condition for loop continuation */
    private String condition;
    /** Maximum iterations (safety guard) */
    private int maxIterations = 100;
    /** Nodes to execute in each loop iteration */
    private List<NodeDefinition> loopBody;

    // --- DELAY ---
    /** Delay duration in seconds */
    private int delaySeconds;

    // --- START ---
    /** Input parameter schema for START nodes.
     *  Key = parameter name, Value = InputParam definition with type/required/default/description. */
    private Map<String, InputParam> inputSchema;

    // --- Common ---
    /** Next node ID (null = end of flow) */
    private String next;

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Object getOutputKey() { return outputKey; }
    public void setOutputKey(Object outputKey) { this.outputKey = outputKey; }

    public List<BranchCase> getBranches() { return branches; }
    public void setBranches(List<BranchCase> branches) { this.branches = branches; }

    public String getDefaultNext() { return defaultNext; }
    public void setDefaultNext(String defaultNext) { this.defaultNext = defaultNext; }

    public String getSignalName() { return signalName; }
    public void setSignalName(String signalName) { this.signalName = signalName; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public String getTimeoutNext() { return timeoutNext; }
    public void setTimeoutNext(String timeoutNext) { this.timeoutNext = timeoutNext; }

    public List<ParallelBranch> getParallelBranches() { return parallelBranches; }
    public void setParallelBranches(List<ParallelBranch> parallelBranches) { this.parallelBranches = parallelBranches; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

    public List<NodeDefinition> getLoopBody() { return loopBody; }
    public void setLoopBody(List<NodeDefinition> loopBody) { this.loopBody = loopBody; }

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

    public Map<String, InputParam> getInputSchema() { return inputSchema; }
    public void setInputSchema(Map<String, InputParam> inputSchema) { this.inputSchema = inputSchema; }

    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }
}
