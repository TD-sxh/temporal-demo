package com.example.temporaldemo.worker.config;

import com.example.temporaldemo.worker.activity.ActivityMeta;
import com.example.temporaldemo.worker.activity.HealthCheckActivitiesImpl;
import com.example.temporaldemo.worker.activity.NotificationActivitiesImpl;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class HealthCheckWorkerConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckWorkerConfig.class);

    @Value("${temporal.server.target:127.0.0.1:7233}")
    private String temporalTarget;

    @Value("${engine.task-queue:orchestrator-task-queue}")
    private String taskQueue;

    @Value("${definition.service.url:http://localhost:8082}")
    private String definitionServiceUrl;

    private WorkflowServiceStubs service;
    private WorkerFactory factory;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalTarget)
                        .build());
        return service;
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(taskQueue);
        worker.registerActivitiesImplementations(
                new HealthCheckActivitiesImpl(),
                new NotificationActivitiesImpl());
        logger.info("Health-check activity Worker registered. Task queue: {}", taskQueue);
        return factory;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (factory != null) {
            factory.start();
            logger.info("Health-check Workers started.");
        }
        registerActivitiesToCatalog();
    }

    private void registerActivitiesToCatalog() {
        // Discover activities by reflecting over all registered activity implementations.
        // For each implementation class: find @ActivityInterface-annotated interfaces,
        // scan their @ActivityMethod methods, and read @ActivityMeta for metadata.
        List<Object> impls = List.of(new HealthCheckActivitiesImpl(), new NotificationActivitiesImpl());
        List<String> activityJsons = new ArrayList<>();

        for (Object impl : impls) {
            for (Class<?> iface : impl.getClass().getInterfaces()) {
                if (!iface.isAnnotationPresent(ActivityInterface.class)) continue;
                for (Method method : iface.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(ActivityMethod.class)) continue;
                    ActivityMeta meta = method.getAnnotation(ActivityMeta.class);
                    String description = meta != null ? meta.description() : "";
                    String outputType  = meta != null ? meta.outputType()  : "object";
                    String inputKeysJson = meta != null
                            ? Arrays.stream(meta.inputKeys())
                                    .map(k -> "\"" + k + "\"")
                                    .collect(Collectors.joining(",", "[", "]"))
                            : "[]";
                    activityJsons.add(
                            "{\"name\":\"%s\",\"description\":\"%s\",\"inputKeys\":%s,\"outputType\":\"%s\"}"
                                    .formatted(method.getName(), description, inputKeysJson, outputType));
                }
            }
        }

        if (activityJsons.isEmpty()) {
            logger.warn("No @ActivityMethod methods found — skipping catalog registration.");
            return;
        }

        String body = "{\"taskQueue\":\"%s\",\"activities\":[%s]}"
                .formatted(taskQueue, String.join(",", activityJsons));
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(definitionServiceUrl + "/api/activities/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            logger.info("Activity catalog registration: {} activities, status={}", activityJsons.size(), resp.statusCode());
        } catch (Exception e) {
            logger.warn("Could not register activities to catalog (non-fatal): {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (factory != null) factory.shutdown();
        if (service != null) service.shutdown();
    }
}
