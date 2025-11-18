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
import com.bumptech.glide.Glide;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 2.0
 * This class is used when the user wants to edit an event.
 */
public class EditEventFragment extends BaseEventFragment {
    private String originalEventId;
    private Event eventToEdit;
    private Button updateButton;

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
        updateButton = view.findViewById(R.id.saveEventButton);
        updateButton.setText("Update Event");
        updateButton.setEnabled(false); // Disable the button by default
        updateButton.setOnClickListener(v -> onSaveOrUpdateButtonClicked(originalEventId));

        populateFields();
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
                        eventToEdit = documentSnapshot.toObject(Event.class);
                        if (eventToEdit == null) return;


                        // Set the ID on the object after loading it
                        eventToEdit.setEventId(documentSnapshot.getId());

                        // --- Populate all fields from the object ---
                        eventNameEditText.setText(eventToEdit.getName());
                        maxPeopleEditText.setText(eventToEdit.getMax_People());
                        dateEditText.setText(eventToEdit.getDate());
                        deadlineEditText.setText(eventToEdit.getDeadline());
                        genreEditText.setText(eventToEdit.getGenre());
                        locationEditText.setText(eventToEdit.getLocation());
                        descriptionEditText.setText(eventToEdit.getDescription());
                        maxentrantEditText.setText(eventToEdit.getWait_List_Maximum());

                        if (eventToEdit.getPosterUrl() != null && !eventToEdit.getPosterUrl().isEmpty()) {
                            Glide.with(getContext()).load(eventToEdit.getPosterUrl()).into(imagePreview);
                            imagePreview.setVisibility(View.VISIBLE);
                        }
                        updateButton.setEnabled(true);
                    }else {
                        // Handle cases where the document might not exist
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
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
     * @param eventFromUI The complete Event object with updated data.
     */
    @Override
    protected void processEvent(Event eventFromUI) {
        // Merge UI changes into the full event object ---
        // If the original event failed to load, we can't safely proceed.
        if (eventToEdit == null) {
            Toast.makeText(getContext(), "Error: Original event not loaded. Cannot save.", Toast.LENGTH_LONG).show();
            if(updateButton != null) {
                updateButton.setEnabled(true);
            }
            return;
        }
        // Copy the editable fields from the UI object to our full object
        eventToEdit.setName(eventFromUI.getName());
        eventToEdit.setMax_People(eventFromUI.getMax_People());
        eventToEdit.setDate(eventFromUI.getDate());
        eventToEdit.setDeadline(eventFromUI.getDeadline());
        eventToEdit.setGenre(eventFromUI.getGenre());
        eventToEdit.setLocation(eventFromUI.getLocation());
        eventToEdit.setDescription(eventFromUI.getDescription());
        eventToEdit.setWait_List_Maximum(eventFromUI.getWait_List_Maximum());

        // Using .set() with an object is often easier than .update() with a map
        if (eventFromUI.getPosterUrl() != null && !eventFromUI.getPosterUrl().isEmpty()) {
            eventToEdit.setPosterUrl(eventFromUI.getPosterUrl());
        }
        // Disable button immediately on click to prevent double-taps
        updateButton.setEnabled(false);

        // Now, save the *merged* object, which contains the old lists and new text fields
        db.collection("Events").document(originalEventId)
                .set(eventToEdit) // Save the complete, updated object
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event updated successfully", Toast.LENGTH_SHORT).show();
                    navigateBack(); // This should now be called correctly
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", "Error updating event", e);
                    Toast.makeText(getContext(), "Failed to update event.", Toast.LENGTH_SHORT).show();
                    // Re-enable button on failure to allow user to try again
                    if(updateButton != null) {
                        updateButton.setEnabled(true);
                    }
                });
    }
}