package com.example.temporaldemo.healthcheck.config;

import com.example.temporaldemo.healthcheck.HealthCheckConstants;
import com.example.temporaldemo.healthcheck.activity.HealthCheckActivitiesImpl;
import com.example.temporaldemo.healthcheck.workflow.HealthCheckWorkflowImpl;
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

/**
 * Core Temporal infrastructure configuration.
 *
 * <p>Creates the {@link WorkflowServiceStubs}, {@link WorkflowClient},
 * and {@link WorkerFactory} beans. The factory is NOT started here —
 * it starts after all workers have been registered (via ContextRefreshedEvent).
 */
@Configuration
public class TemporalConfig {

    private static final Logger logger = LoggerFactory.getLogger(TemporalConfig.class);

    @Value("${temporal.server.target:127.0.0.1:7233}")
    private String temporalTarget;

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
    public WorkflowClient workflowClient(WorkflowServiceStubs service) {
        return WorkflowClient.newInstance(service);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        factory = WorkerFactory.newInstance(client);

        // Register health-check worker
        Worker healthCheckWorker = factory.newWorker(HealthCheckConstants.TASK_QUEUE);
        healthCheckWorker.registerWorkflowImplementationTypes(HealthCheckWorkflowImpl.class);
        healthCheckWorker.registerActivitiesImplementations(new HealthCheckActivitiesImpl());
        logger.info("Health-check Worker registered on task queue: {}", HealthCheckConstants.TASK_QUEUE);

        // NOTE: factory.start() is called in onContextRefreshed() after ALL workers are registered
        return factory;
    }

    /**
     * Start the WorkerFactory after all beans (including engine workers) are initialized.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (factory != null) {
            factory.start();
            logger.info("All Temporal Workers started.");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (factory != null) {
            factory.shutdown();
        }
        if (service != null) {
            service.shutdown();
        }
        logger.info("Temporal resources shut down.");
    }
}
