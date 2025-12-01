package com.example.code_zombom_app.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_zombom_app.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment responsible for fetching and displaying all notification logs from the database.
 * Uses a Firestore Collection Group Query to retrieve notifications from across all events.
 */
public class AdminNotificationLogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AdminNotificationLogAdapter adapter;
    private List<AdminNotificationLog> logList;
    private FirebaseFirestore db;
    private static final String TAG = "AdminNotifLogs";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_notification_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        logList = new ArrayList<>();

        progressBar = view.findViewById(R.id.progress_bar_admin_logs);
        recyclerView = view.findViewById(R.id.recycler_view_admin_logs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminNotificationLogAdapter(logList);
        recyclerView.setAdapter(adapter);

        fetchLogs();
    }

    /**
     * Fetches notification logs using a Collection Group Query.
     * Iterates through all collections named "Notifications" in the Firestore database.
     * <p>
     * Note: This method currently uses debug logging to assist with tracing data issues.
     * Sorting is temporarily disabled to avoid potential missing index errors during debugging.
     */
    private void fetchLogs() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Log.d("DEBUG_LOGS", "Starting to fetch from collectionGroup 'Notifications'...");

        db.collectionGroup("Notifications") // Matches case in Firebase
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.d("DEBUG_LOGS", "Query Successful. Documents found: " + queryDocumentSnapshots.size());

                    logList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d("DEBUG_LOGS", "Found Doc ID: " + doc.getId() + " | Data: " + doc.getData());

                        try {
                            Date date = extractDate(doc.get("createdAt"));
                            String eventName = doc.getString("eventName");
                            String message = doc.getString("message");
                            String email = doc.getString("recipientEmail");
                            String type = doc.getString("type");

                            Log.d("DEBUG_LOGS", "Parsed -> Event: " + eventName + ", Email: " + email);

                            logList.add(new AdminNotificationLog(eventName, message, email, type, date));
                        } catch (Exception e) {
                            Log.e("DEBUG_LOGS", "Error parsing doc: " + doc.getId(), e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (logList.isEmpty()) {
                        Toast.makeText(getContext(), "0 Logs found. Check Logcat.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e("DEBUG_LOGS", "Query FAILED", e);
                    Toast.makeText(getContext(), "Fetch Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Helper method to safely extract a Date object from a Firestore field.
     * Handles cases where the database field might be stored as a Firestore {@link Timestamp}
     * or a primitive {@link Long}.
     *
     * @param raw The raw Object retrieved from the document snapshot.
     * @return A {@link Date} object if extraction is successful, or null otherwise.
     */
    private Date extractDate(@Nullable Object raw) {
        if (raw instanceof Timestamp) {
            return ((Timestamp) raw).toDate();
        }
        if (raw instanceof Long) {
            return new Date((Long) raw);
        }
        return null;
    }
}