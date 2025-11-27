package com.example.code_zombom_app.Helpers.Mail;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that represent a "mail" that can be sent and received.
 *
 * @author Dang Nguyen
 * @version 11/27/2025
 */
public class Mail {
    private String sender;
    private String receiver;
    private String header;
    private String content;
    private MailType mailType;
    private Timestamp timestamp;
    private boolean read;

    /**
     * Interface that help the receiver determine what action to take upon receiving a Mail
     */
    public interface ReceiveAction {
        void onReceive();
    }

    public enum MailType {
        INVITE_LOTTERY_WINNER,
        DECLINE_LOTTERY_LOSER,
        ACCEPT_INVITATION,
        DECLINE_INVITATION
        /* Add more if you feel like it */
    }

    /**
     * No-arg constructor. Firestore use ONLY
     */
    public Mail() {}

    public Mail(MailType type) {
        this.mailType = type;
        this.timestamp = Timestamp.now();
        this.read = false;
    }

    public Mail(String sender, String receiver, MailType type) {
        this(type);
        this.sender = sender;
        this.receiver = receiver;
    }

    public Mail(String sender, String receiver, String header, MailType type) {
        this(type);
        this.sender = sender;
        this.receiver = receiver;
        this.header = header;
    }

    // ---------- Getters / Setters ----------

    public void setSender(String sender) {
        this.sender = sender;
    }
    public String getSender() {
        return sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
    public String getReceiver() {
        return receiver;
    }

    public void setHeader(String header) {
        this.header = header;
    }
    public String getHeader() {
        return header;
    }

    public void setContent(String content) {
        this.content = content;
    }
    public String getContent() {
        return content;
    }

    public void setMailType(MailType mailType) {
        this.mailType = mailType;
    }
    public MailType getMailType() {
        return mailType;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }
    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Send the mail to the entrant.
     * This function will upload this mail onto the database under:
     * Entrants/{receiver}/Mails/{mailId}
     */
    public void send() {
        if (receiver == null || receiver.trim().isEmpty()) {
            Log.e("Mail Sending", "Receiver is null / empty – cannot send mail.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ensure we have a timestamp
        if (timestamp == null) {
            timestamp = Timestamp.now();
        }

        db.collection("Entrants")
                .document(receiver)
                .collection("Mails")
                .add(this)  // auto-generated mailId
                .addOnSuccessListener(docRef -> {
                    Log.i("Mail Sending", "Mail sent successfully: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Mail Sending", "Cannot send this mail", e);
                });
    }

    public void onReceive(ReceiveAction action) {
        action.onReceive();
    }
}
