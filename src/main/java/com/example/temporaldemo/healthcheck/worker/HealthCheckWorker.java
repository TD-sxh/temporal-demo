package com.example.temporaldemo.healthcheck.worker;

import com.example.temporaldemo.healthcheck.HealthCheckConstants;
import com.example.temporaldemo.healthcheck.activity.HealthCheckActivitiesImpl;
import com.example.temporaldemo.healthcheck.workflow.HealthCheckWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HealthCheckWorker {

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget("127.0.0.1:7233")
                        .build());

        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(HealthCheckConstants.TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(HealthCheckWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HealthCheckActivitiesImpl());

        factory.start();
        System.out.println("HealthCheck Worker started. Listening on task queue: " + HealthCheckConstants.TASK_QUEUE);
    }
}
