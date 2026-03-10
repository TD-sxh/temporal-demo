package com.example.temporaldemo.engine.expression;

import com.example.temporaldemo.engine.context.WorkflowContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * Utility for evaluating SpEL expressions against a {@link WorkflowContext}.
 *
 * <p>All workflow variables are exposed as SpEL variables (accessed with # prefix).
 * <pre>
 *   // Given context has variable "severity" = "SEVERE"
 *   boolean result = SpelEvaluator.evaluateBoolean("#severity == 'SEVERE'", context);
 *   // result == true
 * </pre>
 *
 * <p>Also supports string interpolation for TASK input values:
 * <pre>
 *   // Given context has "patientId" = "P001"
 *   Object result = SpelEvaluator.evaluate("#patientId", context);
 *   // result == "P001"
 * </pre>
 */
public class SpelEvaluator {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private SpelEvaluator() {} // utility class

    /**
     * Evaluate a SpEL expression and return the result.
     */
    public static Object evaluate(String expression, WorkflowContext context) {
        EvaluationContext evalCtx = buildEvaluationContext(context);
        Expression expr = PARSER.parseExpression(expression);
        return expr.getValue(evalCtx);
    }

    /**
     * Evaluate a SpEL expression and return a boolean result.
     */
    public static boolean evaluateBoolean(String expression, WorkflowContext context) {
        EvaluationContext evalCtx = buildEvaluationContext(context);
        Expression expr = PARSER.parseExpression(expression);
        Boolean result = expr.getValue(evalCtx, Boolean.class);
        return result != null && result;
    }

    /**
     * Evaluate a SpEL expression and return a String result.
     */
    public static String evaluateString(String expression, WorkflowContext context) {
        EvaluationContext evalCtx = buildEvaluationContext(context);
        Expression expr = PARSER.parseExpression(expression);
        return expr.getValue(evalCtx, String.class);
    }

    /**
     * Resolve input map values: if a value is a String starting with "#",
     * evaluate it as SpEL; otherwise keep as-is.
     */
    public static Map<String, Object> resolveInputs(Map<String, Object> rawInput, WorkflowContext context) {
        if (rawInput == null || rawInput.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> resolved = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawInput.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str && str.startsWith("#")) {
                resolved.put(entry.getKey(), evaluate(str, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private static EvaluationContext buildEvaluationContext(WorkflowContext context) {
        StandardEvaluationContext evalCtx = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : context.getAllVariables().entrySet()) {
            evalCtx.setVariable(entry.getKey(), entry.getValue());
        }
        return evalCtx;
    }
}
