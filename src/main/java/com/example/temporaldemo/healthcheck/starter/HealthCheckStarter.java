package com.example.temporaldemo.healthcheck.starter;

import com.example.temporaldemo.healthcheck.HealthCheckConstants;
import com.example.temporaldemo.healthcheck.model.HealthCheckStatus;
import com.example.temporaldemo.healthcheck.model.PatientVisit;
import com.example.temporaldemo.healthcheck.workflow.HealthCheckWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import java.time.Duration;

public class HealthCheckStarter {

    public static void main(String[] args) throws Exception {
        // Parse optional scenario from args: normal / abnormal / severe / (empty = random)
        String scenario = args.length > 0 ? args[0] : null;
        String patientId = args.length > 1 ? args[1] : "P001";

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget("127.0.0.1:7233")
                        .build());

        WorkflowClient client = WorkflowClient.newInstance(service);

        HealthCheckWorkflow workflow = client.newWorkflowStub(
                HealthCheckWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(HealthCheckConstants.TASK_QUEUE)
                        .setWorkflowId("health-check-" + patientId)
                        .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                        .build());

        PatientVisit visit = new PatientVisit(patientId, "John Doe", "Dr. Smith", "Annual physical exam", scenario);

        System.out.printf("Starting health check workflow for patient %s (scenario=%s)...%n", patientId, scenario);
        System.out.println("Workflow will run asynchronously. Querying status every 2 seconds...");
        System.out.println();

        // Start workflow ASYNCHRONOUSLY
        WorkflowClient.start(workflow::processHealthCheck, visit);

        // Create an untyped stub for getting the result later
        WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);

        // Poll status via Query while workflow is running
        boolean running = true;
        while (running) {
            Thread.sleep(2000);
            try {
                HealthCheckStatus status = workflow.getStatus();
                System.out.println("[Query] " + status);

                if ("COMPLETED".equals(status.getCurrentPhase()) || "CANCELLED".equals(status.getCurrentPhase())) {
                    running = false;
                }
            } catch (Exception e) {
                // Workflow might have completed between query calls
                running = false;
            }
        }

        // Get final result
        String result = untypedStub.getResult(String.class);
        System.out.println();
        System.out.println("=== WORKFLOW RESULT ===");
        System.out.println(result);

        service.shutdown();
    }
}
