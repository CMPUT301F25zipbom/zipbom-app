package com.example.code_zombom_app.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import com.bumptech.glide.Glide;

/**
 * @author Tejwinder Johal
 * @version 1.0
 * This Class is used to display the full details of an event.
 */
public class EventFullDetailsFragment extends Fragment {

    private String eventId;
    private ImageView posterImageView;

    // UI Elements
    private TextView nameValue, dateValue, deadlineValue, locationValue, genreValue,
            maxPeopleValue, waitlistMaxValue, entrantsValue,
            acceptedEntrantsValue, cancelledEntrantsValue, descriptionValue;

    private FirebaseFirestore db;
    private Button returnButton;

    /**
     * This sets up the eventViewModel, database and catches the arguments.
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        // Get the eventId passed from the previous fragment
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    /**
     * Inflates the layout.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_full_event_details, container, false);
    }

    /**
     * This function sets the buttons and textviews to variables.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        initializeViews(view);

        // Find the back button and set its click listener
        Button returnButton = view.findViewById(R.id.returnButton);
        returnButton.setOnClickListener(v -> {
            // Use the NavController to navigate back to the previous screen
            NavHostFragment.findNavController(EventFullDetailsFragment.this).popBackStack();
        });

        // Load the event data from Firestore
        if (eventId != null) {
            loadEventDetails();
        } else {
            Log.e("EventFullDetails", "Event ID is null. Cannot load data.");
            Toast.makeText(getContext(), "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initializes the UI elements.
     * @param view
     */
    private void initializeViews(View view) {
        nameValue = view.findViewById(R.id.name_value);
        dateValue = view.findViewById(R.id.date_value);
        deadlineValue = view.findViewById(R.id.deadline_value);
        locationValue = view.findViewById(R.id.location_value);
        genreValue = view.findViewById(R.id.genre_value);
        maxPeopleValue = view.findViewById(R.id.max_people_value);
        waitlistMaxValue = view.findViewById(R.id.waitlist_max_value);
        entrantsValue = view.findViewById(R.id.entrants_value);
        acceptedEntrantsValue = view.findViewById(R.id.accepted_entrants_value);
        cancelledEntrantsValue = view.findViewById(R.id.cancelled_entrants_value);
        posterImageView = view.findViewById(R.id.poster_image_view);
        descriptionValue = view.findViewById(R.id.description_value);
    }

    /**
     * Fetches the event details from Firestore and populates the UI.
     */
    private void loadEventDetails() {
        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && documentSnapshot.exists()) {
                        // --- REFACTORED: Convert the document directly to an Event object ---
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            populateUi(event);
                        } else {
                            Log.e("EventFullDetails", "Failed to map document to Event object.");
                            Toast.makeText(getContext(), "Error loading event details.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("EventFullDetails", "Document does not exist or fragment not attached.");
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EventFullDetails", "Error fetching event data from Firestore", e);
                    Toast.makeText(getContext(), "Failed to load event.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Populates the UI elements with data from the document.
     * @param event
     */
    private void populateUi(Event event) {
        // Set simple string values
        nameValue.setText(event.getName());
        dateValue.setText(event.getDate());
        deadlineValue.setText(event.getDeadline());
        locationValue.setText(event.getLocation());
        genreValue.setText(event.getGenre());
        maxPeopleValue.setText(event.getMax_People());
        waitlistMaxValue.setText(event.getWait_List_Maximum());
        descriptionValue.setText(event.getDescription());


        // Set array values
        entrantsValue.setText(formatListToString((List<String>) event.getEntrants()));
        acceptedEntrantsValue.setText(formatListToString((List<String>) event.getAccepted_Entrants()));
        cancelledEntrantsValue.setText(formatListToString((List<String>) event.getCancelled_Entrants()));

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getPosterUrl())
                    .into(posterImageView);
            posterImageView.setVisibility(View.VISIBLE);
        } else {
            posterImageView.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method to format a list of strings into a single comma-separated string.
     * @param list The list to format.
     * @return A formatted string, or "None" if the list is null or empty.
     */
    private String formatListToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "None";
        }
        // This requires Java 8+ (API level 24+)
        return TextUtils.join(", ", list);
    }
}
