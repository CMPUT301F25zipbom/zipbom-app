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

/**
 * Admin fragment for managing Events.
 * Allows administrators to view a list of all events, view event details,
 * and delete events from the database.
 */
public class EventsAdminFragment extends Fragment {

    public LinearLayout eventsContainer;
    private FirebaseFirestore db;
    private CollectionReference eventsdb;
    private EventService eventService = new EventService();

    /**
     * Creates and configures the root view for the fragment.
     * Sets up a ScrollView containing a linear layout to hold the dynamic list of events.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

    /**
     * Called immediately after the view has been created.
     * Initializes the Firestore connection and begins listening for database updates.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        eventsdb = db.collection("Events");
        loadEventsFromDatabase();
    }

    /**
     * Dependency injection method for testing purposes.
     * Allows setting a mock Firestore instance.
     *
     * @param mockDb               The mock FirebaseFirestore instance.
     * @param mockEventsCollection The mock CollectionReference.
     */
    public void setMockDatabase(FirebaseFirestore mockDb, CollectionReference mockEventsCollection) {
        this.db = mockDb;
        this.eventsdb = mockEventsCollection;
    }

    /**
     * Sets up a real-time listener on the 'Events' collection.
     * Automatically updates the UI whenever events are added, removed, or modified.
     */
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
                    String eventDetails = formatEventString(snapshot);

                    View eventView = safeInflater.inflate(R.layout.admin_event_list_item, eventsContainer, false);

                    TextView eventTextView = eventView.findViewById(R.id.textView_event_list_items_details);
                    ImageButton deleteButton = eventView.findViewById(R.id.button_delete_event);

                    eventTextView.setText(eventDetails);

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
     * Helper method to format event data into a readable string.
     * Extracts fields such as name, capacity, dates, and location from the Firestore document.
     *
     * @param snapshot The QueryDocumentSnapshot containing event data.
     * @return A formatted string representation of the event.
     */
    private String formatEventString(QueryDocumentSnapshot snapshot) {
        String name = snapshot.getString("name");
        String genre = snapshot.getString("genre");

        Long capacity = snapshot.getLong("capacity");

        Timestamp startTs = snapshot.getTimestamp("eventStartDate");
        Timestamp endTs = snapshot.getTimestamp("eventEndDate");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);
        String startStr = (startTs != null) ? sdf.format(startTs.toDate()) : "N/A";
        String endStr = (endTs != null) ? sdf.format(endTs.toDate()) : "N/A";

        StringBuilder locBuilder = new StringBuilder();
        Object locObj = snapshot.get("location");
        if (locObj instanceof Map) {
            Map<String, Object> locMap = (Map<String, Object>) locObj;
            if (locMap.get("street") != null) locBuilder.append(locMap.get("street"));
            if (locMap.get("city") != null) locBuilder.append(", ").append(locMap.get("city"));
        } else {
            locBuilder.append("Unknown Location");
        }

        return "Name: " + (name != null ? name : "Unknown") + "\n" +
                "Capacity: " + (capacity != null ? capacity : 0) + "\n" +
                "Date: " + startStr + "\n" +
                "Deadline: " + endStr + "\n" +
                "Genre: " + (genre != null ? genre : "N/A") + "\n" +
                "Location: " + locBuilder.toString();
    }

    /**
     * Displays a confirmation dialog before deleting an event.
     *
     * @param snapshot The document snapshot of the event to be deleted.
     */
    private void showDeleteConfirmationDialog(QueryDocumentSnapshot snapshot) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> showReasonInputDialog(snapshot))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays a dialog requesting a reason for the deletion.
     * If confirmed, the event is deleted via the EventService.
     *
     * @param snapshot The document snapshot of the event to be deleted.
     */
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

    /**
     * Displays detailed information about a selected event in a custom dialog.
     *
     * @param snapshot The document snapshot containing the event details.
     */
    private void showEventDetailsDialog(QueryDocumentSnapshot snapshot) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.unsure_event_pop_up, null);

        TextView title = dialogView.findViewById(R.id.event_details_title);
        TextView body = dialogView.findViewById(R.id.event_details_body);
        View closeBtn = dialogView.findViewById(R.id.button_close_event_dialog);

        String baseDetails = formatEventString(snapshot);
        String description = snapshot.getString("description");
        String fullDetails = baseDetails + "\nDescription: " + (description != null ? description : "None");

        if (body != null) {
            body.setText(fullDetails);
        }

        if (title != null) {
            String name = snapshot.getString("name");
            title.setText("Details: " + (name != null ? name : "Event"));
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}