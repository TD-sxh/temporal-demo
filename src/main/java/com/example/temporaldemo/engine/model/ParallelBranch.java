package com.example.temporaldemo.engine.model;

import java.util.List;

/**
 * A branch inside a PARALLEL node. Contains a list of {@link NodeDefinition}
 * that are executed sequentially within this branch, while all branches run
 * concurrently.
 *
 * <p>JSON example:
 * <pre>
 * {
 *   "branchId": "blood_test",
 *   "nodes": [
 *     { "id": "draw_blood", "type": "TASK", "activityName": "drawBlood", ... },
 *     { "id": "analyze_blood", "type": "TASK", "activityName": "analyzeBlood", ... }
 *   ]
 * }
 * </pre>
 */
public class ParallelBranch {

    /** Unique ID for this parallel branch */
    private String branchId;

    /** Sequential nodes within this branch */
    private List<NodeDefinition> nodes;

    public ParallelBranch() {}

    public ParallelBranch(String branchId, List<NodeDefinition> nodes) {
        this.branchId = branchId;
        this.nodes = nodes;
    }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public List<NodeDefinition> getNodes() { return nodes; }
    public void setNodes(List<NodeDefinition> nodes) { this.nodes = nodes; }
}
