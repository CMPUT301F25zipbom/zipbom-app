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

    public String getNotificationtype() {
        return notificationtype;
    }

    public void setNotificationtype(String notificationtype) {
        this.notificationtype = notificationtype;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getReciever() {
        return reciever;
    }

    public void setReciever(String reciever) {
        this.reciever = reciever;
    }
}
