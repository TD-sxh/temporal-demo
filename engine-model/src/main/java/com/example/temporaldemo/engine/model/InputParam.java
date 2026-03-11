package com.example.temporaldemo.engine.model;

/**
 * Schema definition for a single input parameter of a START node.
 *
 * <p>JSON example:
 * <pre>
 * {
 *   "name": "patientId",
 *   "type": "string",
 *   "required": true,
 *   "defaultValue": null,
 *   "description": "Patient ID"
 * }
 * </pre>
 */
public class InputParam {

    /** Parameter name (unique within the schema) */
    private String name;

    /** Data type hint: string, number, boolean, object */
    private String type = "string";

    /** Whether this parameter is required to trigger the flow */
    private boolean required = true;

    /** Default value if not provided (only meaningful for optional params) */
    private Object defaultValue;

    /** Human-readable description */
    private String description;

    public InputParam() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
