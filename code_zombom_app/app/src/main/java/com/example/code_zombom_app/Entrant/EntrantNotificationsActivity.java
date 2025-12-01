package com.example.code_zombom_app.Entrant;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a list of notifications targeted to the signed-in entrant.
 */
public class EntrantNotificationsActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "Email";

    private static final String PREFS_COLLECTION = "NotificationPreferences";
    private final ArrayList<EntrantNotification> notifications = new ArrayList<>();
    private ArrayAdapter<EntrantNotification> adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private String email;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_notifications_activity);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        firestore = FirebaseFirestore.getInstance();

        ListView listView = findViewById(R.id.list_notifications);
        progressBar = findViewById(R.id.progress_notifications);
        emptyView = findViewById(R.id.text_notifications_empty);

        adapter = new ArrayAdapter<EntrantNotification>(this,
                R.layout.entrant_notification_item,
                R.id.text_notification_title,
                notifications) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull android.view.ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                EntrantNotification n = getItem(position);
                if (n != null) {
                    TextView title = row.findViewById(R.id.text_notification_title);
                    TextView subtitle = row.findViewById(R.id.text_notification_subtitle);
                    title.setText(n.title);
                    String time = n.createdAt == null
                            ? getString(R.string.history_unknown_date)
                            : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                            .format(n.createdAt);
                    subtitle.setText(time + " • " + n.message);
                    row.setAlpha(n.seen ? 0.6f : 1f);
                }
                return row;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            EntrantNotification n = notifications.get(position);
            showNotificationDialog(n);
        });

        loadNotifications();
    }

    private void loadNotifications() {
        if (email == null || email.trim().isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.history_missing_email);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resolveNotificationPreference(email.trim(), enabled -> {
            if (!enabled) {
                notifications.clear();
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(R.string.notifications_disabled_message);
                return;
            }

            firestore.collectionGroup("Notifications")
                    .whereEqualTo("recipientEmail", email.trim().toLowerCase())
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        notifications.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Date createdAt = extractDate(doc.get("createdAt"));
                            notifications.add(new EntrantNotification(
                                    doc.getId(),
                                    doc.getReference(),
                                    doc.getString("eventId"),
                                    doc.getString("type"),
                                    doc.getString("eventName"),
                                    doc.getString("message"),
                                    createdAt,
                                    Boolean.TRUE.equals(doc.getBoolean("seen")),
                                    Boolean.TRUE.equals(doc.getBoolean("responded"))
                            ));
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        emptyView.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText(R.string.history_load_error);
                    });
        });
    }

    private void resolveNotificationPreference(@NonNull String rawEmail,
                                               @NonNull PreferenceCallback callback) {
        String trimmed = rawEmail.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);

        DocumentReference prefRef = firestore.collection(PREFS_COLLECTION).document(normalized);
        prefRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean enabled = snapshot.getBoolean("notificationEnabled");
                        callback.onResolved(enabled == null || enabled);
                    } else {
                        resolveFromProfiles(trimmed, normalized, callback);
                    }
                })
                .addOnFailureListener(e -> resolveFromProfiles(trimmed, normalized, callback));
    }

    private void resolveFromProfiles(@NonNull String trimmed,
                                     @NonNull String normalized,
                                     @NonNull PreferenceCallback callback) {
        fetchProfilePreference(trimmed, callback, () ->
                fetchProfilePreference(normalized, callback, () -> callback.onResolved(true)));
    }

    private void fetchProfilePreference(@NonNull String documentId,
                                        @NonNull PreferenceCallback callback,
                                        @NonNull Runnable fallback) {
        if (documentId.isEmpty()) {
            fallback.run();
            return;
        }
        firestore.collection("Profiles").document(documentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean enabled = snapshot.getBoolean("notificationEnabled");
                        if (enabled != null) {
                            callback.onResolved(enabled);
                        } else {
                            fallback.run();
                        }
                    } else {
                        fallback.run();
                    }
                })
                .addOnFailureListener(e -> fallback.run());
    }

    private interface PreferenceCallback {
        void onResolved(boolean enabled);
    }

    private void markSeen(@NonNull EntrantNotification n) {
        n.seen = true;
        adapter.notifyDataSetChanged();
        if (n.ref != null) {
            n.ref.update("seen", true);
        }
    }

    private Date extractDate(@Nullable Object raw) {
        if (raw instanceof Timestamp) {
            return ((Timestamp) raw).toDate();
        }
        if (raw instanceof Long) {
            return new Date((Long) raw);
        }
        return null;
    }

    private void showNotificationDialog(@NonNull EntrantNotification n) {
        if (isInvitation(n.type) && n.eventId != null && !n.eventId.trim().isEmpty() && !n.responded) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(n.title)
                    .setMessage(n.message)
                    .setPositiveButton(R.string.accept, (d, w) -> handleInvitationResponse(n, true))
                    .setNegativeButton(R.string.decline, (d, w) -> handleInvitationResponse(n, false))
                    .setNeutralButton(android.R.string.ok, (d, w) -> {
                        markSeen(n);
                        d.dismiss();
                    })
                    .show();
        } else {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(n.title)
                    .setMessage(n.message)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        markSeen(n);
                        d.dismiss();
                    })
                    .show();
        }
    }

    private void handleInvitationResponse(@NonNull EntrantNotification n, boolean accept) {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, R.string.history_missing_email, Toast.LENGTH_SHORT).show();
            return;
        }
        EventService service = new EventService();
        if (accept) {
            service.acceptInvitation(n.eventId, email)
                    .addOnSuccessListener(ignored -> {
                        markInvitationHandled(n);
                        Toast.makeText(this, R.string.accept, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : getString(R.string.history_load_error),
                            Toast.LENGTH_SHORT).show());
        } else {
            service.declineInvitation(n.eventId, email)
                    .addOnSuccessListener(ignored -> {
                        markInvitationHandled(n);
                        Toast.makeText(this, R.string.decline, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : getString(R.string.history_load_error),
                            Toast.LENGTH_SHORT).show());
        }
        markSeen(n);
    }

    private boolean isInvitation(@Nullable String type) {
        if (type == null) return false;
        return "win".equalsIgnoreCase(type) || "org_selected".equalsIgnoreCase(type);
    }

    private static class EntrantNotification {
        final String id;
        final com.google.firebase.firestore.DocumentReference ref;
        final String eventId;
        final String type;
        final String title;
        final String message;
        final Date createdAt;
        boolean seen;
        boolean responded;

        EntrantNotification(String id,
                            com.google.firebase.firestore.DocumentReference ref,
                            String eventId,
                            String type,
                            String title,
                            String message,
                            Date createdAt,
                            boolean seen,
                            boolean responded) {
            this.id = id == null ? "" : id;
            this.ref = ref;
            this.eventId = eventId == null ? "" : eventId;
            this.type = type == null ? "" : type;
            this.title = (title == null || title.trim().isEmpty()) ? "Notification" : title;
            this.message = (message == null || message.trim().isEmpty()) ? "" : message;
            this.createdAt = createdAt;
            this.seen = seen;
            this.responded = responded;
        }

        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }

    private void markInvitationHandled(@NonNull EntrantNotification n) {
        n.responded = true;
        if (n.ref != null) {
            n.ref.update("responded", true);
        }
    }
}
