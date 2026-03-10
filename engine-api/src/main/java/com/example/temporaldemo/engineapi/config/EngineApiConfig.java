package com.example.temporaldemo.engineapi.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineApiConfig {

    private static final Logger logger = LoggerFactory.getLogger(EngineApiConfig.class);

    @Value("${temporal.server.target:127.0.0.1:7233}")
    private String temporalTarget;

    private WorkflowServiceStubs service;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalTarget)
                        .build());
        logger.info("Connected to Temporal at {}", temporalTarget);
        return service;
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs);
    }

    @PreDestroy
    public void shutdown() {
        if (service != null) service.shutdown();
    }
}
