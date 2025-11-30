package com.example.code_zombom_app.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class EventsAdminFragment extends Fragment {

    public LinearLayout eventsContainer;
    private FirebaseFirestore db;
    private CollectionReference eventsdb;
    private EventService eventService = new EventService();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Root layout setup
        LinearLayout rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        eventsContainer = new LinearLayout(getContext());
        eventsContainer.setOrientation(LinearLayout.VERTICAL);
        eventsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollView.addView(eventsContainer);
        rootLayout.addView(scrollView);
        return rootLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        eventsdb = db.collection("Events");
        loadEventsFromDatabase();
    }

    public void setMockDatabase(FirebaseFirestore mockDb, CollectionReference mockEventsCollection) {
        this.db = mockDb;
        this.eventsdb = mockEventsCollection;
    }

    private void loadEventsFromDatabase() {
        eventsdb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            if (!isAdded() || getContext() == null) {
                return;
            }

            eventsContainer.removeAllViews();
            LayoutInflater safeInflater = LayoutInflater.from(getContext());

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {

                    // We cannot use EventForOrg here because the field names don't match.

                    String eventDetails = formatEventString(snapshot);

                    View eventView = safeInflater.inflate(R.layout.event_admin_list_item, eventsContainer, false);

                    TextView eventTextView = eventView.findViewById(R.id.textView_event_list_items_details);
                    ImageButton deleteButton = eventView.findViewById(R.id.button_delete_event);

                    eventTextView.setText(eventDetails);

                    // Click listeners
                    eventTextView.setOnClickListener(v -> showEventDetailsDialog(snapshot));
                    deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(snapshot));

                    eventsContainer.addView(eventView);
                }
            } else {
                TextView noEvents = new TextView(getContext());
                noEvents.setText("No events yet.");
                noEvents.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                noEvents.setTextSize(18);
                noEvents.setPadding(16, 16, 16, 16);
                eventsContainer.addView(noEvents);
            }
        });
    }

    /**
     * Helper method to extract fields using the EXACT keys from your Firebase Data
     */
    private String formatEventString(QueryDocumentSnapshot snapshot) {
        // 1. Strings (using lowercase keys from your data)
        String name = snapshot.getString("name");
        String genre = snapshot.getString("genre");
        String description = snapshot.getString("description");

        // 2. Numbers (capacity is a number in your DB, not a string)
        Long capacity = snapshot.getLong("capacity");

        // 3. Timestamps (Your DB uses Timestamp objects, not strings)
        Timestamp startTs = snapshot.getTimestamp("eventStartDate");
        Timestamp endTs = snapshot.getTimestamp("eventEndDate");

        // Format Dates
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);
        String startStr = (startTs != null) ? sdf.format(startTs.toDate()) : "N/A";
        String endStr = (endTs != null) ? sdf.format(endTs.toDate()) : "N/A";

        // 4. Location (Your DB has a Map, not a String)
        StringBuilder locBuilder = new StringBuilder();
        Object locObj = snapshot.get("location");
        if (locObj instanceof Map) {
            Map<String, Object> locMap = (Map<String, Object>) locObj;
            // Extract specific fields from the location map
            if (locMap.get("street") != null) locBuilder.append(locMap.get("street"));
            if (locMap.get("city") != null) locBuilder.append(", ").append(locMap.get("city"));
        } else {
            locBuilder.append("Unknown Location");
        }

        // Build the string
        return "Name: " + (name != null ? name : "Unknown") + "\n" +
                "Capacity: " + (capacity != null ? capacity : 0) + "\n" +
                "Date: " + startStr + "\n" +
                "Deadline: " + endStr + "\n" +
                "Genre: " + (genre != null ? genre : "N/A") + "\n" +
                "Location: " + locBuilder.toString();
    }

    private void showDeleteConfirmationDialog(QueryDocumentSnapshot snapshot) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> showReasonInputDialog(snapshot))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showReasonInputDialog(QueryDocumentSnapshot snapshot) {
        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Enter reason for deleting this event");

        new AlertDialog.Builder(getContext())
                .setTitle("Reason for Deletion")
                .setMessage("Please specify why you are deleting this event:")
                .setView(input)
                .setPositiveButton("Confirm Delete", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(getContext(), "Deletion cancelled — reason required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    eventService.deleteEvent(snapshot.getId())
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Event deleted.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete event.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEventDetailsDialog(QueryDocumentSnapshot snapshot) {
        // 1. Inflate the custom dark layout
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.event_pop_up, null);

        // 2. Find Views inside the new layout
        TextView title = dialogView.findViewById(R.id.event_details_title);
        TextView body = dialogView.findViewById(R.id.event_details_body);
        View closeBtn = dialogView.findViewById(R.id.button_close_event_dialog);

        // 3. Prepare the data string using your formatting logic
        // We reuse the existing formatEventString method, then add the description
        String baseDetails = formatEventString(snapshot);
        String description = snapshot.getString("description");

        // Add ID or other specific fields if needed
        String fullDetails = baseDetails + "\nDescription: " + (description != null ? description : "None");

        // 4. Set the text
        if (body != null) {
            body.setText(fullDetails);
        }

        // Optional: Set title with Event Name if you want
        if (title != null) {
            String name = snapshot.getString("name");
            title.setText("Details: " + (name != null ? name : "Event"));
        }

        // 5. Create and Show the Dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView) // This is important! Sets the custom view
                .create();

        // 6. Handle the Close button
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
