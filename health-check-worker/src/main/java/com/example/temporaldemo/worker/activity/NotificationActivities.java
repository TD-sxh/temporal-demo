package com.example.temporaldemo.worker.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Notification activities for HUMAN_TASK nodes.
 *
 * <p>Each method receives a generic input map containing:
 * <ul>
 *   <li>{@code taskId}     — the HUMAN_TASK node id</li>
 *   <li>{@code taskName}   — the human-readable node name</li>
 *   <li>{@code workflowId} — the enclosing workflow id</li>
 *   <li>{@code to}         — recipient email (EMAIL channel)</li>
 *   <li>{@code phone}      — recipient phone number (SMS / PHONE channels)</li>
 *   <li>{@code message}    — the notification body</li>
 * </ul>
 *
 * <p>Implementations are demo stubs that log the notification instead of
 * sending to a real provider.
 */
@ActivityInterface
public interface NotificationActivities {

    /**
     * Send an email notification.
     * Activity name: {@code sendNotificationEmail}
     */
    @ActivityMethod
    @ActivityMeta(description = "Sends a digital message email notification",
            inputKeys = {"to", "message", "taskName", "workflowId"})
    Object sendNotificationEmail(Map<String, Object> input);

    /**
     * Send an SMS notification.
     * Activity name: {@code sendNotificationSms}
     */
    @ActivityMethod
    @ActivityMeta(description = "Sends a digital message SMS notification",
            inputKeys = {"phone", "message", "taskName", "workflowId"})
    Object sendNotificationSms(Map<String, Object> input);

    /**
     * Trigger a phone call notification.
     * Activity name: {@code sendNotificationCall}
     */
    @ActivityMethod
    @ActivityMeta(description = "Sends a digital message phone notification",
            inputKeys = {"phone", "message", "taskName", "workflowId"})
    Object sendNotificationCall(Map<String, Object> input);
}
