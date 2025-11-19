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

import java.util.ArrayList;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.DocumentReference;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This class is responsible for creating a new event, making sure the event is valid and saving it to firebase
 */
public class AddEventFragment extends BaseEventFragment {
    /**
     * This method gets the data from the user and creates a new event if data is valid.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button saveButton = view.findViewById(R.id.saveEventButton);
        saveButton.setText("Save Event");

        saveButton.setOnClickListener(v -> {
            // Create a new empty document to get a unique ID
            DocumentReference newEventRef = db.collection("Events").document();
            String newEventId = newEventRef.getId();

            // Call the shared save/update handler from the base class
            onSaveOrUpdateButtonClicked(newEventId);
        });
    }
    /**
     * REFACTORED: Implements the abstract method to create a new Firestore document
     * directly from the Event object.
     * @param event The complete Event object to be saved.
     */
    @Override
    protected void processEvent(com.example.code_zombom_app.organizer.Event event) {
        // Add fields that are specific to a new event
        event.setEntrants(new ArrayList<>());
        event.setCancelled_Entrants(new ArrayList<>());
        event.setAccepted_Entrants(new ArrayList<>());
        event.setLottery_Winners(new ArrayList<>());

        // --- REFACTORED: Pass the entire Event object to .set() ---
        db.collection("Events").document(event.getEventId())
                .set(event) // Firestore will automatically map the Event object to a document
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", "Error creating event", e);
                    Toast.makeText(getContext(), "Error creating event.", Toast.LENGTH_SHORT).show();
                });
    }
}