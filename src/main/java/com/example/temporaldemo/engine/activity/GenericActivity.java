package com.example.temporaldemo.engine.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Generic Temporal Activity that delegates execution to registered activity handlers.
 *
 * <p>Instead of having one Temporal Activity interface per business action,
 * we use a single generic interface. The {@code activityName} parameter
 * identifies which handler to invoke from the {@link ActivityHandlerRegistry}.
 */
@ActivityInterface
public interface GenericActivity {

    /**
     * Execute a named activity with the given input parameters.
     *
     * @param activityName the registered name of the activity handler
     * @param input        key-value input parameters
     * @return result value (can be any serializable object)
     */
    @ActivityMethod
    Object execute(String activityName, Map<String, Object> input);
}
