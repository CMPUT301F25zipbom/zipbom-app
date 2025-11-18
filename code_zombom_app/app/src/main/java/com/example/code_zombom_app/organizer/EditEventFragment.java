package com.example.code_zombom_app.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.code_zombom_app.R;
import java.util.Map;
import com.bumptech.glide.Glide;
import com.example.code_zombom_app.organizer.Event;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This class is used when the user wants to edit an event.
 */
public class EditEventFragment extends BaseEventFragment {
    private String originalEventId;
//    private String originalEventText;

    /**
     * This sets up the eventViewModel, database and catches the arguments.
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.  Also Initialize the
     * image picker launcher to get a image for the poster.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            originalEventId = getArguments().getString("eventId");
        }
    }

    /**
     * This function sets the buttons and textviews to variables. It then calls populate fields to fill in the textboxes
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        populateFields();

        Button updateButton = view.findViewById(R.id.saveEventButton);
        updateButton.setText("Update Event");
        updateButton.setOnClickListener(v -> onSaveOrUpdateButtonClicked(originalEventId));
    }

    /**
     * REFACTORED: This function gets the event from Firestore, converts it to an Event object,
     * and populates the fields.
     */
    private void populateFields() {
        if (originalEventId == null) return;
        // Populate fields that require a full Firestore document read (Description, Waitlist, Poster)
        db.collection("Events").document(originalEventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        // --- REFACTORED: Convert document to Event object ---
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event == null) return;

                        // --- Populate all fields from the object ---
                        eventNameEditText.setText(event.getName());
                        maxPeopleEditText.setText(event.getMax_People());
                        dateEditText.setText(event.getDate());
                        deadlineEditText.setText(event.getDeadline());
                        genreEditText.setText(event.getGenre());
                        locationEditText.setText(event.getLocation());
                        descriptionEditText.setText(event.getDescription());
                        maxentrantEditText.setText(event.getWait_List_Maximum());

                        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                            Glide.with(getContext()).load(event.getPosterUrl()).into(imagePreview);
                            imagePreview.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load event data.", Toast.LENGTH_SHORT).show();
                    Log.e("FIRESTORE_ERROR", "Error loading event for edit", e);
                });
    }

    /**
     * REFACTORED: Implements the abstract method to update an existing Firestore document
     * directly from the Event object.
     * @param event The complete Event object with updated data.
     */
    @Override
    protected void processEvent(Event event) {
        // --- REFACTORED: Pass the entire Event object to .set() or .update() ---
        // Using .set() with an object is often easier than .update() with a map
        db.collection("Events").document(event.getEventId())
                .set(event) // .set() will overwrite the document with the object's state
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event updated successfully", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", "Error updating event", e);
                    Toast.makeText(getContext(), "Failed to update event.", Toast.LENGTH_SHORT).show();
                });
    }
}