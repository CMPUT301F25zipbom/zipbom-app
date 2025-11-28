package com.example.code_zombom_app.Helpers.Mail;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that provides mail services, such as send/receive mail.
 *
 * Uses Firestore collection "Mails" where each document is one Mail.
 *
 * @author Dang Nguyen
 * @version 11/27/2025
 * @see Mail
 */
public class MailService {

    /**
     * Callback interface for async mail list operations.
     */
    public interface MailListCallback {
        void onSuccess(List<Mail> mails);
        void onError(Exception e);
    }

    /**
     * Send a mail. Thin wrapper around Mail.send().
     *
     * @param mail The mail to send.
     */
    public static void sendMail(Mail mail) {
        mail.send();
    }

    /**
     * One-shot: fetch all mails for a receiver once (no realtime updates).
     *
     * @param receiver email of the receiver
     * @param callback callback invoked when data loads or fails
     */
    public static void getAllMailOnce(String receiver, MailListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Mails")
                .whereEqualTo("receiver", receiver)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mail> mails = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Mail mail = doc.toObject(Mail.class);
                        if (mail != null) {
                            // Store doc id in the Mail object if you need it later (for delete / mark read)
                            mail.setId(doc.getId());
                            mails.add(mail);
                        }
                    }
                    callback.onSuccess(mails);
                })
                .addOnFailureListener(e -> {
                    Log.e("MailService", "Failed to load mails", e);
                    callback.onError(e);
                });
    }

    /**
     * Realtime: listen to mail updates for a receiver.<br><br>
     *
     * Every time a mail is added/updated/deleted for this receiver, the callback
     * is invoked with the current list sorted by timestamp descending.
     *
     * @param receiver email (must match Mail.receiver field)
     * @param callback callback invoked anytime the mailbox changes
     * @return ListenerRegistration so caller can remove listener in onStop()/onDestroy()
     */
    public static ListenerRegistration listenToMailUpdates(
            String receiver,
            MailListCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection("Mails")
                .whereEqualTo("receiver", receiver)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("MailService", "listenToMailUpdates error", e);
                        callback.onError(e);
                        return;
                    }
                    if (querySnapshot == null) {
                        callback.onSuccess(new ArrayList<Mail>());
                        return;
                    }

                    List<Mail> mails = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Mail mail = doc.toObject(Mail.class);
                        if (mail != null) {
                            mail.setId(doc.getId());
                            mails.add(mail);
                        }
                    }
                    callback.onSuccess(mails);
                });
    }

    /**
     * Mark a mail as read in Firestore.
     *
     * @param mailId Firestore document id of the mail
     */
    public static void markMailAsRead(String mailId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Mails")
                .document(mailId)
                .update("read", true)
                .addOnSuccessListener(aVoid ->
                        Log.i("MailService", "Mail " + mailId + " marked as read"))
                .addOnFailureListener(e ->
                        Log.e("MailService", "Failed to mark mail as read", e));
    }

    /**
     * Delete a mail document entirely.
     *
     * @param mailId Firestore document id of the mail
     */
    public static void deleteMail(String mailId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Mails")
                .document(mailId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.i("MailService", "Mail " + mailId + " deleted"))
                .addOnFailureListener(e ->
                        Log.e("MailService", "Failed to delete mail", e));
    }
}
