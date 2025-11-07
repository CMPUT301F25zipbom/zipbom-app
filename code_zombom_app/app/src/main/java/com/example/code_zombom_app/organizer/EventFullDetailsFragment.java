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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.stream.Collectors;
import com.bumptech.glide.Glide;

public class EventFullDetailsFragment extends Fragment {

    private static final String TAG = "EventFullDetails";
    private String eventId;
    private ImageView posterImageView;

    // UI Elements
    private TextView nameValue, dateValue, deadlineValue, locationValue, genreValue,
            maxPeopleValue, waitlistMaxValue, entrantsValue,
            acceptedEntrantsValue, cancelledEntrantsValue, descriptionValue;

    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        // Get the eventId passed from the previous fragment
        if (getArguments() != null) {
            this.eventId = getArguments().getString("eventId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_full_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize all TextViews
        initializeViews(view);

        Button returnButton = view.findViewById(R.id.returnButton);

        // Fetch and display the data if eventId is available
        if (eventId != null && !eventId.isEmpty()) {
            loadEventDetails();
        } else {
            Log.e(TAG, "Event ID is null. Cannot display details.");
            // Optionally show an error message on screen
        }
        returnButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(EventFullDetailsFragment.this).navigateUp();
        });
    }

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

    private void loadEventDetails() {
        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        populateUi(documentSnapshot);
                    } else {
                        Log.w(TAG, "No such document with ID: " + eventId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching document", e));
    }

    private void populateUi(DocumentSnapshot doc) {
        // Set simple string values
        nameValue.setText(doc.getString("Name"));
        dateValue.setText(doc.getString("Date"));
        deadlineValue.setText(doc.getString("Deadline"));
        locationValue.setText(doc.getString("Location"));
        genreValue.setText(doc.getString("Genre"));
        maxPeopleValue.setText(doc.getString("Max People"));
        waitlistMaxValue.setText(doc.getString("Wait List Maximum"));
        descriptionValue.setText(doc.getString("Description"));


        // Set array values
        entrantsValue.setText(formatListToString((List<String>) doc.get("Entrants")));
        acceptedEntrantsValue.setText(formatListToString((List<String>) doc.get("Accepted Entrants")));
        cancelledEntrantsValue.setText(formatListToString((List<String>) doc.get("Cancelled Entrants")));

        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
//                    .placeholder(R.drawable.your_placeholder) // Optional: show while loading
//                    .error(R.drawable.your_placeholder)       // Optional: show on error
                    .into(posterImageView);
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
