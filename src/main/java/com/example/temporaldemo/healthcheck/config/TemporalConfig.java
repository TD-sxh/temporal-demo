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
        Worker worker = factory.newWorker(HealthCheckConstants.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HealthCheckWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HealthCheckActivitiesImpl());
        factory.start();
        logger.info("Temporal Worker started. Listening on task queue: {}", HealthCheckConstants.TASK_QUEUE);
        return factory;
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
