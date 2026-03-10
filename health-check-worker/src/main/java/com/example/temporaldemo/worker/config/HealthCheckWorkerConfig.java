package com.example.temporaldemo.worker.config;

import com.example.temporaldemo.engine.activity.ActivityHandlerRegistry;
import com.example.temporaldemo.engine.activity.GenericActivityImpl;
import com.example.temporaldemo.engine.handlers.*;
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
public class HealthCheckWorkerConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckWorkerConfig.class);

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
    public ActivityHandlerRegistry activityHandlerRegistry() {
        ActivityHandlerRegistry registry = new ActivityHandlerRegistry();
        registry.register("recordVisit", new RecordVisitHandler());
        registry.register("getDiagnosisResult", new GetDiagnosisHandler());
        registry.register("analyzeResults", new AnalyzeResultsHandler());
        registry.register("notifyDoctor", new NotifyDoctorHandler());
        registry.register("sendEmail", new SendEmailHandler());
        registry.register("sendSms", new SendSmsHandler());
        registry.register("processLabSignal", new ProcessLabSignalHandler());
        registry.register("performFollowUp", new PerformFollowUpHandler());
        registry.register("updateFollowUpState", new UpdateFollowUpStateHandler());
        registry.register("buildSummary", new BuildSummaryHandler());
        return registry;
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client, ActivityHandlerRegistry registry) {
        factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(taskQueue);
        worker.registerActivitiesImplementations(new GenericActivityImpl(registry));
        logger.info("Health-check activity Worker registered. Task queue: {}", taskQueue);
        return factory;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (factory != null) {
            factory.start();
            logger.info("Health-check Workers started.");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (factory != null) factory.shutdown();
        if (service != null) service.shutdown();
    }
}
