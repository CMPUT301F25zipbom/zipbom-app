package com.example.code_zombom_app.ui.admin;

import java.util.Date;

/**
 * Data model representing a single notification log entry for the Admin interface.
 * Stores details about the event, message content, recipient, and creation time.
 */
public class AdminNotificationLog {
    private String eventName;
    private String message;
    private String recipientEmail;
    private String type;
    private Date createdAt;

    /**
     * Constructs a new AdminNotificationLog.
     *
     * @param eventName      The name of the event associated with the notification.
     * @param message        The body content of the notification.
     * @param recipientEmail The email address of the user who received the notification.
     * @param type           The type/category of notification (e.g., "win", "loss").
     * @param createdAt      The date and time the notification was created.
     */
    public AdminNotificationLog(String eventName, String message, String recipientEmail, String type, Date createdAt) {
        this.eventName = eventName;
        this.message = message;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.createdAt = createdAt;
    }

    /**
     * Gets the name of the event.
     * @return The event name string.
     */
    public String getEventName() { return eventName; }

    /**
     * Gets the notification message.
     * @return The message string.
     */
    public String getMessage() { return message; }

    /**
     * Gets the recipient's email address.
     * @return The email string.
     */
    public String getRecipientEmail() { return recipientEmail; }

    /**
     * Gets the type of notification.
     * @return The notification type string.
     */
    public String getType() { return type; }

    /**
     * Gets the creation timestamp.
     * @return A Date object representing when the notification was sent.
     */
    public Date getCreatedAt() { return createdAt; }
}