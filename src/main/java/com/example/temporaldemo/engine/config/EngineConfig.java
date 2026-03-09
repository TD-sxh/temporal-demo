package com.example.temporaldemo.engine.config;

import com.example.temporaldemo.engine.activity.ActivityHandlerRegistry;
import com.example.temporaldemo.engine.activity.GenericActivityImpl;
import com.example.temporaldemo.engine.handlers.*;
import com.example.temporaldemo.engine.workflow.OrchestratorWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the workflow orchestration engine.
 *
 * <p>Registers:
 * <ul>
 *   <li>The {@link ActivityHandlerRegistry} with all health-check handlers</li>
 *   <li>A new Temporal Worker listening on the engine task queue</li>
 *   <li>The {@link OrchestratorWorkflowImpl} and {@link GenericActivityImpl}</li>
 * </ul>
 */
@Configuration
public class EngineConfig {

    private static final Logger logger = LoggerFactory.getLogger(EngineConfig.class);

    @Value("${engine.task-queue:orchestrator-task-queue}")
    private String taskQueue;

    /**
     * Activity handler registry with all business activity handlers registered.
     */
    @Bean
    public ActivityHandlerRegistry activityHandlerRegistry() {
        ActivityHandlerRegistry registry = new ActivityHandlerRegistry();

        // Health check handlers
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

    /**
     * Register the engine worker with the existing WorkerFactory.
     *
     * <p>Uses the same {@link WorkerFactory} created by the health check config,
     * but registers a new Worker on a different task queue.
     */
    @Bean
    public Worker engineWorker(WorkerFactory workerFactory, ActivityHandlerRegistry registry) {
        Worker worker = workerFactory.newWorker(taskQueue);
        worker.registerWorkflowImplementationTypes(OrchestratorWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GenericActivityImpl(registry));
        logger.info("Engine Worker registered. Task queue: {}", taskQueue);
        return worker;
    }
}
