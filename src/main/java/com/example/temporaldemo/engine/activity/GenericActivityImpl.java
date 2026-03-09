package com.example.temporaldemo.engine.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of {@link GenericActivity} that delegates to the
 * {@link ActivityHandlerRegistry}.
 *
 * <p>This is the single Temporal Activity implementation registered
 * with the Worker. All TASK nodes go through this.
 */
public class GenericActivityImpl implements GenericActivity {

    private static final Logger logger = LoggerFactory.getLogger(GenericActivityImpl.class);

    private final ActivityHandlerRegistry registry;

    public GenericActivityImpl(ActivityHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object execute(String activityName, Map<String, Object> input) {
        logger.info("Executing activity '{}' with input keys: {}", activityName,
                input != null ? input.keySet() : "null");
        ActivityHandler handler = registry.getHandler(activityName);
        Object result = handler.handle(input != null ? input : Map.of());
        logger.info("Activity '{}' completed. Result: {}", activityName, result);
        return result;
    }
}
