package com.example.code_zombom_app.organizer;

public class NotificationTemplate {
    private String notificationtype;
    private String reciever;
    private String time;

    public NotificationTemplate(String notificationtype, String reciever, String time) {
        this.notificationtype = notificationtype;
        this.reciever = reciever;
        this.time = time;
    }
}
