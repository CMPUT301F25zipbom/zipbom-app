package com.example.code_zombom_app.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminFragment extends Fragment {

    private LinearLayout eventsContainer;
    private FirebaseFirestore db;
    private CollectionReference eventsdb;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // parent linearlayout
        LinearLayout rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#4CAF50")); // green background
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Container to hold events
        eventsContainer = new LinearLayout(getContext());
        eventsContainer.setOrientation(LinearLayout.VERTICAL);
        eventsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        rootLayout.addView(eventsContainer);

        return rootLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        eventsdb = db.collection("Events");

        loadEventsFromDatabase();
    }

    private void loadEventsFromDatabase() {
        eventsdb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            eventsContainer.removeAllViews();

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    StringBuilder eventText = new StringBuilder();
                    eventText.append("Name: ").append(snapshot.getString("Name")).append("\n")
                            .append("Max People: ").append(snapshot.getString("Max People")).append("\n")
                            .append("Date: ").append(snapshot.getString("Date")).append("\n")
                            .append("Deadline: ").append(snapshot.getString("Deadline")).append("\n")
                            .append("Genre: ").append(snapshot.getString("Genre")).append("\n");
                    if (snapshot.getString("Location") != null) {
                        eventText.append("Location: ").append(snapshot.getString("Location"));
                    }

                    // Inflate admin event item layout
                    View eventView = LayoutInflater.from(getContext())
                            .inflate(R.layout.event_admin_list_item, eventsContainer, false);

                    TextView eventTextView = eventView.findViewById(R.id.event_item_textview);
                    ImageButton deleteButton = eventView.findViewById(R.id.button_delete_event);

                    eventTextView.setText(eventText.toString());

                    // Click to view event details
                    eventTextView.setOnClickListener(v -> showEventDetailsDialog(snapshot));

                    // Click trash button to delete
                    deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(snapshot));

                    eventsContainer.addView(eventView);
                }
            } else {
                TextView noEvents = new TextView(getContext());
                noEvents.setText("No events yet.");
                noEvents.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                noEvents.setTextSize(18);
                noEvents.setPadding(16,16,16,16);
                eventsContainer.addView(noEvents);
            }
        });
    }

    private void showDeleteConfirmationDialog(QueryDocumentSnapshot snapshot) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete from Firestore
                    eventsdb.document(snapshot.getId()).delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEventDetailsDialog(QueryDocumentSnapshot snapshot) {
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(snapshot.getString("Name")).append("\n")
                .append("Max People: ").append(snapshot.getString("Max People")).append("\n")
                .append("Date: ").append(snapshot.getString("Date")).append("\n")
                .append("Deadline: ").append(snapshot.getString("Deadline")).append("\n")
                .append("Genre: ").append(snapshot.getString("Genre")).append("\n");
        if (snapshot.getString("Location") != null) {
            details.append("Location: ").append(snapshot.getString("Location"));
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Event Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
