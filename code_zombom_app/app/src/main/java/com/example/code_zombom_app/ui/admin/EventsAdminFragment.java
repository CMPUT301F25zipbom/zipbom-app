package com.example.code_zombom_app.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.EventForOrg;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * This {@code AdminFragment} class gives an admin UI
 * that allows an app administrator to view and delete event data stored in Firestore.
 * (will add more features once implemented later)
 * <p>
 * This fragment displays a scrollable list of all events fetched from the database
 * in Firestore. Each event displays its details, along with a trash button to delete it.
 * Admin users can tap an event to view detailed information or press the delete icon
 * to remove it after confirmation.
 * </p>
 */
public class EventsAdminFragment extends Fragment {


    /** container that holds all event views */
    public LinearLayout eventsContainer;

    /** reference to the firestore database */
    private FirebaseFirestore db;

    /** reference to the firestore collection that stores events */
    private CollectionReference eventsdb;
    private EventService eventService = new EventService();

    /**
     * called when the fragment's UI is being created.
     * Background that wraps the event list in a scrollable view,
     *
     * @param inflater  LayoutInflater object for inflating views
     * @param container Parent view that the fragment UI will be attached to
     * @param savedInstanceState Previous saved state if any
     * @return The root view containing the admin layout
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // the root layout
        LinearLayout rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // a ScrollView to make the event list scrollable
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // container to hold individual event items
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
     * Initializes Firestore and loads all event data from the database.
     *
     * @param view               The fragment view
     * @param savedInstanceState Saved state if the fragment is being re-created
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize Firestore
        db = FirebaseFirestore.getInstance();
        eventsdb = db.collection("Events");

        // display all events
        loadEventsFromDatabase();
    }

    // Allows tests to provide a fake Firestore instance
    public void setMockDatabase(FirebaseFirestore mockDb, CollectionReference mockEventsCollection) {
        this.db = mockDb;
        this.eventsdb = mockEventsCollection;
    }


    /**
     * Fetches event data from Firestore and populates the UI and
     * adds a listener to update the event list in real time whenever changes occur
     */
    private void loadEventsFromDatabase() {
        eventsdb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            eventsContainer.removeAllViews();

            // check if Firestore returned any data
            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {

                    // event description text (prefer canonical mapping to stay in sync with organiser/entrant flows)
                    StringBuilder eventText = new StringBuilder();
                    try {
                        EventForOrg dto = snapshot.toObject(EventForOrg.class);
                        Event event = EventMapper.toDomain(dto, snapshot.getId());
                        if (event != null) {
                            eventText.append("Name: ").append(event.getName()).append("\n")
                                    .append("Max People: ").append(event.getCapacity()).append("\n")
                                    .append("Date: ").append(event.getEventStartDate().toString()).append("\n")
                                    .append("Deadline: ").append(event.getEventEndDate().toString()).append("\n")
                                    .append("Genre: ");
                            if (!event.getGenre().trim().isEmpty()) {
                                eventText.append(event.getGenre());
                            }
                            if (event.getLocation() != null && !event.getLocation().toString().trim().isEmpty()) {
                                eventText.append("\nLocation: ").append(event.getLocation().toString());
                            }
                        } else {
                            appendRawFields(snapshot, eventText);
                        }
                    } catch (Exception ex) {
                        Log.w("AdminMapping", "Falling back to raw fields for " + snapshot.getId(), ex);
                        appendRawFields(snapshot, eventText);
                    }

                    View eventView = LayoutInflater.from(getContext())
                            .inflate(R.layout.event_admin_list_item, eventsContainer, false);

                    // get text and delete button views
                    TextView eventTextView = eventView.findViewById(R.id.textView_event_list_items_details);
                    ImageButton deleteButton = eventView.findViewById(R.id.button_delete_event);

                    // display event information
                    eventTextView.setText(eventText.toString());

                    // tap event to view details
                    eventTextView.setOnClickListener(v -> showEventDetailsDialog(snapshot));

                    // tap trash button to confirm and delete event
                    deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(snapshot));

                    // add event view to the container
                    eventsContainer.addView(eventView);
                }
            } else {
                // give message if there are no events in Firestore
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
     * Builds a fallback event string using the raw Firestore fields when mapping fails.
     */
    private void appendRawFields(QueryDocumentSnapshot snapshot, StringBuilder eventText) {
        eventText.append("Name: ").append(snapshot.getString("Name")).append("\n")
                .append("Max People: ").append(snapshot.getString("Max People")).append("\n")
                .append("Date: ").append(snapshot.getString("Date")).append("\n")
                .append("Deadline: ").append(snapshot.getString("Deadline")).append("\n")
                .append("Genre: ").append(snapshot.getString("Genre")).append("\n");
        if (snapshot.getString("Location") != null) {
            eventText.append("Location: ").append(snapshot.getString("Location"));
        }
    }

    /**
     * Displays a confirmation dialog before deleting the selected event.
     * If confirmed, deletes the event document from Firestore.
     *
     * @param snapshot The Firestore document representing the selected event
     */
    private void showDeleteConfirmationDialog(QueryDocumentSnapshot snapshot) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showReasonInputDialog(snapshot);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Prompts the admin to enter a reason for deletion after confirming.
     *
     * @param snapshot The Firestore document representing the selected event
     */
    private void showReasonInputDialog(QueryDocumentSnapshot snapshot) {
        // Create input field
        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Enter reason for deleting this event");

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Reason for Deletion")
                .setMessage("Please specify why you are deleting this event:")
                .setView(input)
                .setPositiveButton("Confirm Delete", (dialog, which) -> {
                    String reason = input.getText().toString().trim();

                    if (reason.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Deletion cancelled — reason required.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Log or store the reason
                    Log.d("AdminDelete", "Deleting event " + snapshot.getId() + " for reason: " + reason);

                    eventService.deleteEvent(snapshot.getId())
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(),
                                            "Event deleted. Reason: " + reason,
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed to delete event.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays a dialog showing the information about a selected event
     *
     * @param snapshot The Firestore document representing the event
     */
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

        // display the dialog
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Event Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
