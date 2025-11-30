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

import com.example.code_zombom_app.ui.admin.AdminNotificationLog;
import com.example.code_zombom_app.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private void fetchLogs() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Log.d("DEBUG_LOGS", "Starting to fetch from collectionGroup 'Notifications'...");

        db.collectionGroup("Notifications") // Ensure this matches the Case in Firebase
                .get() // Removed .orderBy for now to rule out Index issues
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.d("DEBUG_LOGS", "Query Successful. Documents found: " + queryDocumentSnapshots.size());

                    logList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Print the raw data of every document found
                        Log.d("DEBUG_LOGS", "Found Doc ID: " + doc.getId() + " | Data: " + doc.getData());

                        try {
                            Date date = extractDate(doc.get("createdAt"));
                            String eventName = doc.getString("eventName");
                            String message = doc.getString("message");
                            String email = doc.getString("recipientEmail");
                            String type = doc.getString("type");

                            // Log the parsed values
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
     * LOGIC COPIED FROM ENTRANT CODE
     * Handles cases where 'createdAt' is a Number OR a Firestore Timestamp
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