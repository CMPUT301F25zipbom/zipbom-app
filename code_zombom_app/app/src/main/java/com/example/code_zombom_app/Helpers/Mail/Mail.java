package com.example.code_zombom_app.Helpers.Mail;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

/**
 * A class that represents a "mail" that can be sent and received.
 *
 * Stored in Firestore in the "Mails" collection, one document per Mail.
 *
 * @author Dang Nguyen
 * @version 11/27/2025
 */
public class Mail {

    private String id;          // Unique mail's id
    private String sender;      // sender email
    private String receiver;    // receiver email
    private String header;      // mail subject
    private String content;     // mail body
    private MailType mailType;  // semantic type
    private Timestamp timestamp; // when mail was sent
    private boolean read;       // has the receiver opened/read it?

    /**
     * Interface that helps the receiver determine what action to take upon receiving a Mail.
     * (You can use this app-side, not stored in Firestore.)
     */
    public interface ReceiveAction {
        void onReceive();
    }

    public enum MailType {
        INVITE_LOTTERY_WINNER,
        DECLINE_LOTTERY_LOSER,
        ACCEPT_INVITATION,
        DECLINE_INVITATION,
        EDITED_EVENT
        // Add more if needed
    }

    /**
     * No-arg constructor. Required by Firestore.
     */
    public Mail() {
        // Firestore only
    }

    public Mail(MailType type) {
        this.mailType = type;
        this.read = false;
        this.id = UUID.randomUUID().toString();
    }

    public Mail(String sender, String receiver, MailType type) {
        this(type);
        this.sender = sender;
        this.receiver = receiver;
    }

    public Mail(String sender,
                String receiver,
                String header,
                String content,
                MailType type) {
        this(type);
        this.sender = sender;
        this.receiver = receiver;
        this.header = header;
        this.content = content;
    }

    // region Getters / Setters

    public String getId() {
        return id;
    }

    /**
     * Set the Firestore document id. This is typically set when reading from Firestore.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set the sender's email.
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * @return The sender's email address.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Set the receiver's email.
     */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    /**
     * @return The receiver's email address.
     */
    public String getReceiver() {
        return receiver;
    }

    /**
     * Set the mail's header/subject.
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * @return The mail's header.
     */
    public String getHeader() {
        return header;
    }

    /**
     * Set the mail's content/body.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return The mail's content/body.
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the mail type. Firestore uses this via the default setter.
     */
    public void setMailType(MailType mailType) {
        this.mailType = mailType;
    }

    /**
     * @return The mail's type.
     */
    public MailType getMailType() {
        return mailType;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Set the mail timestamp. Usually set automatically when sending.
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return true if the mail is marked as read.
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Mark this mail as read/unread.
     */
    public void setRead(boolean read) {
        this.read = read;
    }

    // endregion

    /**
     * Send the mail: writes a new document to the "Mails" collection in Firestore.
     *
     * This uses the "single collection" pattern:
     *   collection("Mails") -> one document per mail, with a "receiver" field.
     */
    public void send() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Set timestamp if not already set
        if (timestamp == null) {
            timestamp = Timestamp.now();
        }

        // "read" is false by default when sending
        this.read = false;

        db.collection("Mails")
                .document(receiver)
                .set(this)
                .addOnSuccessListener(ref -> {
                    Log.i("Mail", "Mail sent successfully: ");
                })
                .addOnFailureListener(e -> {
                    Log.e("Mail", "Cannot send this mail", e);
                });

        /*
         * If you want to trigger a push notification:
         * - Write a Cloud Function that listens to onCreate of documents in "Mails"
         * - In that function, look up the receiver's FCM token and send an FCM message
         * - The Android app handles FCM in FirebaseMessagingService and shows a notification
         */
    }

    /**
     * Convenience method to run some app-side logic when this mail is "received".
     * (Pure Java callback; not tied to Firestore. You can use this in your UI code.)
     */
    public void onReceive(ReceiveAction action) {
        action.onReceive();
    }
}
