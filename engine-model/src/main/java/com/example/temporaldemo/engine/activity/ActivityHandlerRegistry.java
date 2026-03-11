package com.example.temporaldemo.engine.activity;

import com.example.temporaldemo.engine.model.ActivityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that maps activity names to their handler implementations.
 *
 * <p>At application startup, all {@link ActivityHandler} beans register
 * themselves here. The {@link GenericActivityImpl} looks up handlers
 * by name at runtime.
 */
public class ActivityHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ActivityHandlerRegistry.class);

    private final Map<String, ActivityHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, ActivityInfo> infos = new ConcurrentHashMap<>();

    /**
     * Register an activity handler with optional metadata.
     */
    public void register(String name, ActivityHandler handler, ActivityInfo info) {
        handlers.put(name, handler);
        if (info != null) {
            info.setName(name);
            infos.put(name, info);
        }
        logger.info("Registered activity handler: {}", name);
    }

    /** Register without metadata (backward compat). */
    public void register(String name, ActivityHandler handler) {
        register(name, handler, null);
    }

    /**
     * Look up a handler by name.
     *
     * @throws IllegalArgumentException if no handler is registered with that name
     */
    public ActivityHandler getHandler(String name) {
        ActivityHandler handler = handlers.get(name);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No activity handler registered with name: '" + name +
                    "'. Available handlers: " + handlers.keySet());
        }
        return handler;
    }

    /**
     * Check if a handler exists.
     */
    public boolean hasHandler(String name) {
        return handlers.containsKey(name);
    }

    public Map<String, ActivityHandler> getAllHandlers() {
        return Map.copyOf(handlers);
    }

    public List<ActivityInfo> getAllActivityInfos() {
        return new ArrayList<>(infos.values());
    }
}
