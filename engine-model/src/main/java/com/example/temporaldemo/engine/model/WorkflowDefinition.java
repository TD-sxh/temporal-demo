package com.example.temporaldemo.engine.model;

import java.util.List;
import java.util.Map;

/**
 * Top-level definition of a JSON-driven workflow.
 *
 * <p>This is the root object deserialized from a workflow JSON file.
 * It contains metadata plus an ordered list of {@link NodeDefinition} nodes
 * that form the execution graph.
 *
 * <p>JSON example:
 * <pre>
 * {
 *   "id": "health-check-flow",
 *   "name": "Health Check Workflow",
 *   "version": 1,
 *   "startNode": "record_visit",
 *   "initialVariables": {
 *     "patientId": "",
 *     "patientName": "",
 *     "maxFollowUps": 5
 *   },
 *   "nodes": [ ... ]
 * }
 * </pre>
 */
public class WorkflowDefinition {

    /** Unique identifier for this workflow definition */
    private String id;

    /** Human-readable name */
    private String name;

    /** Version number (for future versioning support) */
    private int version = 1;

    /** ID of the first node to execute */
    private String startNode;

    /** Initial variables to seed into the workflow context */
    private Map<String, Object> initialVariables;

    /** All nodes in this workflow (order doesn't matter; traversal is by id references) */
    private List<NodeDefinition> nodes;

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getStartNode() { return startNode; }
    public void setStartNode(String startNode) { this.startNode = startNode; }

    public Map<String, Object> getInitialVariables() { return initialVariables; }
    public void setInitialVariables(Map<String, Object> initialVariables) { this.initialVariables = initialVariables; }

    public List<NodeDefinition> getNodes() { return nodes; }
    public void setNodes(List<NodeDefinition> nodes) { this.nodes = nodes; }
}
