package com.example.code_zombom_app.Entrant;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a simple chronological list of an entrant's event history.
 */
public class EntrantHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "Email";

    private final ArrayList<HistoryEntry> historyEntries = new ArrayList<>();
    private ArrayAdapter<HistoryEntry> adapter;
    private ProgressBar progressBar;
    private TextView emptyView;

    /**
     * Inflates the history layout and wires an ArrayAdapter that renders status and timestamp
     * for each historical entry.
     *
     * @param savedInstanceState previously saved instance state
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_history);

        ListView listView = findViewById(R.id.list_history);
        progressBar = findViewById(R.id.progress_history);
        emptyView = findViewById(R.id.text_history_empty);

        adapter = new ArrayAdapter<HistoryEntry>(this, R.layout.item_entrant_history, R.id.text_history_title, historyEntries) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull android.view.ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                HistoryEntry entry = getItem(position);
                if (entry != null) {
                    TextView title = row.findViewById(R.id.text_history_title);
                    TextView subtitle = row.findViewById(R.id.text_history_subtitle);
                    title.setText(entry.eventName.isEmpty() ? getString(R.string.history_unknown_event) : entry.eventName);
                    String formattedDate = entry.updatedAt == null
                            ? getString(R.string.history_unknown_date)
                            : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                            .format(entry.updatedAt);
                    subtitle.setText(getString(R.string.history_status_format, entry.status, formattedDate));
                }
                return row;
            }
        };

        listView.setAdapter(adapter);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.trim().isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.history_missing_email);
            progressBar.setVisibility(View.GONE);
            return;
        }
        loadHistory(email.trim());
    }

    /**
     * Loads entrant history from Firestore ordered by the most recent update and displays it
     * in the list. Shows an empty or error state on failure.
     *
     * @param email entrant identifier used as the Profile document id
     */
    private void loadHistory(@NonNull String email) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance()
                .collection("Profiles")
                .document(email)
                .collection("History")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    historyEntries.clear();
                    querySnapshot.getDocuments().forEach(doc -> {
                        HistoryEntry entry = new HistoryEntry(
                                doc.getString("eventId"),
                                doc.getString("eventName"),
                                doc.getString("status"),
                                doc.getDate("updatedAt")
                        );
                        historyEntries.add(entry);
                    });
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(historyEntries.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(R.string.history_load_error);
                });
    }

    /**
     * Simple immutable model consumed by the history list adapter. Mirrors the fields stored
     * under Profiles/{email}/History in Firestore so binders can map status and timestamps.
     */
    private static class HistoryEntry {
        final String eventId;
        final String eventName;
        final String status;
        final Date updatedAt;

        /**
         * Lightweight model used by the ListView adapter.
         *
         * @param eventId   Firestore event id
         * @param eventName cached name at time of write (may be empty)
         * @param status    latest status label
         * @param updatedAt timestamp of the status update
         */
        HistoryEntry(String eventId, String eventName, String status, Date updatedAt) {
            this.eventId = eventId == null ? "" : eventId;
            this.eventName = eventName == null ? "" : eventName;
            this.status = status == null ? "" : status;
            this.updatedAt = updatedAt;
        }

        @NonNull
        @Override
        public String toString() {
            return eventName + " • " + status;
        }
    }
}
