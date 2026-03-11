package com.example.temporaldemo.worker.config;

import com.example.temporaldemo.engine.activity.ActivityHandlerRegistry;
import com.example.temporaldemo.engine.activity.GenericActivityImpl;
import com.example.temporaldemo.engine.handlers.*;
import com.example.temporaldemo.engine.model.ActivityInfo;
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

import java.util.List;

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
        registry.register("recordVisit", new RecordVisitHandler(),
                ActivityInfo.of("Records a patient visit and returns a visitId",
                        List.of("patientId", "patientName", "doctorName", "visitReason"), "string"));
        registry.register("getDiagnosisResult", new GetDiagnosisHandler(),
                ActivityInfo.of("Retrieves diagnosis result for a patient",
                        List.of("patientId", "scenario"), "map"));
        registry.register("analyzeResults", new AnalyzeResultsHandler(),
                ActivityInfo.of("Analyzes diagnosis and returns severity level",
                        List.of("diagnosis"), "string"));
        registry.register("notifyDoctor", new NotifyDoctorHandler(),
                ActivityInfo.of("Sends a notification to the attending doctor",
                        List.of("doctorName", "patientId", "severity"), "string"));
        registry.register("sendEmail", new SendEmailHandler(),
                ActivityInfo.of("Sends an email notification with diagnosis details",
                        List.of("doctorName", "patientId", "diagnosis"), "map"));
        registry.register("sendSms", new SendSmsHandler(),
                ActivityInfo.of("Sends an SMS alert for severe cases",
                        List.of("doctorName", "patientId", "severity"), "map"));
        registry.register("processLabSignal", new ProcessLabSignalHandler(),
                ActivityInfo.of("Processes an incoming lab result signal",
                        List.of("labSignal", "currentScore"), "number"));
        registry.register("performFollowUp", new PerformFollowUpHandler(),
                ActivityInfo.of("Performs a follow-up check for the patient",
                        List.of("patientId", "followUpCount", "severity", "currentScore"), "map"));
        registry.register("updateFollowUpState", new UpdateFollowUpStateHandler(),
                ActivityInfo.of("Updates follow-up counters and severity after each iteration",
                        List.of("followUpResult", "followUpCount"), "map"));
        registry.register("buildSummary", new BuildSummaryHandler(),
                ActivityInfo.of("Builds a final summary report for the visit",
                        List.of("patientId", "severity", "followUpCount", "visitId"), "map"));
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
