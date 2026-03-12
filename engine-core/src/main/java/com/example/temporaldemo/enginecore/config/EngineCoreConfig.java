package com.example.temporaldemo.enginecore.config;

import com.example.temporaldemo.engine.workflow.DigitalMessageWorkflowImpl;
import com.example.temporaldemo.engine.workflow.OrchestratorWorkflowImpl;
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

@Configuration
public class EngineCoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(EngineCoreConfig.class);

    @Value("${temporal.server.target:127.0.0.1:7233}")
    private String temporalTarget;

    @Value("${engine.task-queue:orchestrator-task-queue}")
    private String taskQueue;

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
        worker.registerWorkflowImplementationTypes(
                OrchestratorWorkflowImpl.class,
                DigitalMessageWorkflowImpl.class);
        logger.info("Engine Worker registered (workflow only). Task queue: {}", taskQueue);
        return factory;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (factory != null) {
            factory.start();
            logger.info("Engine Core Workers started.");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (factory != null) factory.shutdown();
        if (service != null) service.shutdown();
    }
}
