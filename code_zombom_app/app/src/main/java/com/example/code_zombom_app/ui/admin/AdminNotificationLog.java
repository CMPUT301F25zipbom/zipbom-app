package com.example.code_zombom_app.ui.admin;

import java.util.Date;

public class AdminNotificationLog {
    private String eventName;
    private String message;
    private String recipientEmail;
    private String type;
    private Date createdAt; // Changed from long to Date to handle both formats

    public AdminNotificationLog(String eventName, String message, String recipientEmail, String type, Date createdAt) {
        this.eventName = eventName;
        this.message = message;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.createdAt = createdAt;
    }

    public String getEventName() { return eventName; }
    public String getMessage() { return message; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getType() { return type; }
    public Date getCreatedAt() { return createdAt; }
}