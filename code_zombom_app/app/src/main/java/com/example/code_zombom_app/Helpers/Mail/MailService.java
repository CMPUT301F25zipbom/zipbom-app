package com.example.code_zombom_app.Helpers.Mail;

import android.util.Log;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A class that provide the mail services, such as send/receive mail, delete email, etc
 *
 * @author Dang Nguyen
 * @version 11/27/2025
 * @see Mail
 */
public class MailService {

    public interface MailListCallback {
        void onSuccess(List<Mail> mails);
        void onError(Exception e);
    }

    /**
     * Send a mail
     *
     * @param mail The mail to send
     */
    public static void sendMail(Mail mail) {
        mail.send();
    }

    /**
     * One-shot: fetch all mails for a receiver once.
     *
     * @param receiver email of the receiver (also Entrants/{receiver} doc id)
     * @param callback callback invoked when data loads or fails
     */
    public static void getAllMailOnce(String receiver, MailListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Entrants")
                .document(receiver)
                .collection("Mails")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mail> mails = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Mail mail = doc.toObject(Mail.class);
                        if (mail != null) {
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
     * Realtime: listen to mail updates for a receiver. This means everytime there is a change in
     * the mail database this function get notified in real time
     *
     * @param receiver email / doc id of the receiver
     * @param callback callback invoked anytime the mailbox changes
     * @return ListenerRegistration so caller can remove listener in onStop()
     */
    public static ListenerRegistration listenToMailUpdates(
            String receiver,
            MailListCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection("Entrants")
                .document(receiver)
                .collection("Mails")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("MailService", "listenToMailUpdates error", e);
                        callback.onError(e);
                        return;
                    }
                    if (querySnapshot == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Mail> mails = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Mail mail = doc.toObject(Mail.class);
                        if (mail != null) {
                            mails.add(mail);
                        }
                    }
                    callback.onSuccess(mails);
                });
    }
}
