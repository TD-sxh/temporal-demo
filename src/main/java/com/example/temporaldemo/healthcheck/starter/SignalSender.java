package com.example.temporaldemo.healthcheck.starter;

import com.example.temporaldemo.healthcheck.model.HealthCheckStatus;
import com.example.temporaldemo.healthcheck.workflow.HealthCheckWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

/**
 * Sends signals to a running HealthCheckWorkflow.
 *
 * Usage:
 *   cancel  [workflowId] [reason]         - cancel follow-ups
 *   labresult [workflowId] [score]         - inject new lab result (0.0~1.0)
 *   query   [workflowId]                   - query current status
 *
 * Examples:
 *   cancel health-check-P001 "Patient refused"
 *   labresult health-check-P001 0.9
 *   query health-check-P001
 */
public class SignalSender {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  cancel    <workflowId> [reason]");
            System.out.println("  labresult <workflowId> <score>");
            System.out.println("  query     <workflowId>");
            return;
        }

        String action = args[0];
        String workflowId = args[1];

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget("127.0.0.1:7233")
                        .build());

        WorkflowClient client = WorkflowClient.newInstance(service);

        // Get a stub for an EXISTING running workflow by its ID
        HealthCheckWorkflow workflow = client.newWorkflowStub(HealthCheckWorkflow.class, workflowId);

        switch (action.toLowerCase()) {
            case "cancel":
                String reason = args.length > 2 ? args[2] : "Cancelled by operator";
                workflow.cancelFollowUp(reason);
                System.out.printf("Signal 'cancelFollowUp' sent to workflow %s. Reason: %s%n", workflowId, reason);
                break;

            case "labresult":
                if (args.length < 3) {
                    System.out.println("Error: labresult requires a score (0.0~1.0)");
                    break;
                }
                double score = Double.parseDouble(args[2]);
                workflow.newLabResult(score);
                System.out.printf("Signal 'newLabResult' sent to workflow %s. Score: %.2f%n", workflowId, score);
                break;

            case "query":
                HealthCheckStatus status = workflow.getStatus();
                System.out.println("Current workflow status:");
                System.out.println(status);
                break;

            default:
                System.out.println("Unknown action: " + action);
                System.out.println("Valid actions: cancel, labresult, query");
        }

        service.shutdown();
    }
}
