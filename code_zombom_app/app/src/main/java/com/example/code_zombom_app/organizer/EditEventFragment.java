package com.example.code_zombom_app.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditEventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private FirebaseFirestore db;
    private String originalEventId;
    private String originalEventText; // The full text of the event
    private CollectionReference eventref;
    private FirebaseFirestore db1;
    private CollectionReference events;

    private EditText eventNameEditText, maxPeopleEditText, dateEditText, deadlineEditText, genreEditText, locationEditText, maxentrantEditText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        db = FirebaseFirestore.getInstance();

        // Retrieve the arguments passed from the previous fragment
        if (getArguments() != null) {
            originalEventId = getArguments().getString("eventId");
            originalEventText = getArguments().getString("eventText");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db1 = FirebaseFirestore.getInstance();
        events = db1.collection("Events");

        eventref = db1.collection("Events");


        Button cancelButton = view.findViewById(R.id.cancelButton);

        // Find all EditTexts
        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        dateEditText = view.findViewById(R.id.editTextDate);
        deadlineEditText = view.findViewById(R.id.editTextDeadline);
        genreEditText = view.findViewById(R.id.editTextGenre);
        locationEditText = view.findViewById(R.id.editTextLocation);
        maxentrantEditText = view.findViewById(R.id.maxamountofentrants);

        // Store original texts incase of a cancel
        Map<String, Object> updatedEventDataIfCancel = new HashMap<>();
        String ogName = eventNameEditText.getText().toString();
        String ogMaxPeople = maxPeopleEditText.getText().toString();
        String ogDate = dateEditText.getText().toString();
        String ogDeadline = deadlineEditText.getText().toString();
        String ogGenre = genreEditText.getText().toString();
        String ogLocation = locationEditText.getText().toString();

        // Pre-fill the fields with existing data
        populateFields();

        cancelButton.setOnClickListener(v -> {
            updatedEventDataIfCancel.put("Name", ogName);
            updatedEventDataIfCancel.put("Max People", ogMaxPeople);
            updatedEventDataIfCancel.put("Date", ogDate);
            updatedEventDataIfCancel.put("Deadline", ogDeadline);
            updatedEventDataIfCancel.put("Genre", ogGenre);
            if (!ogLocation.isEmpty()){
                updatedEventDataIfCancel.put("Location", ogLocation);
            }
            NavHostFragment.findNavController(EditEventFragment.this).navigateUp();
        });

        Button updateButton = view.findViewById(R.id.saveEventButton);
        updateButton.setOnClickListener(v -> updateEvent());
    }

    private void populateFields() {
        // A more robust solution would be a proper data model.
        if (originalEventText == null) return;

        // Setting Wait List Maximum text
        if (originalEventId != null) {
            db.collection("Events").document(originalEventId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Check if the field exists in the document
                            if (documentSnapshot.contains("Wait List Maximum")) {
                                String waitListMax = documentSnapshot.getString("Wait List Maximum");
                                maxentrantEditText.setText(waitListMax);
                            }
                        }
                    });
        }

        String[] lines = originalEventText.split("\n");
        for (String line : lines) {
            String[] parts = line.split(": ", 2);
            if (parts.length < 2) continue;

            String key = parts[0];
            String value = parts[1];

            if ("Name".equals(key)) eventNameEditText.setText(value);
            else if ("Max People".equals(key)) maxPeopleEditText.setText(value);
            else if ("Date".equals(key)) dateEditText.setText(value);
            else if ("Deadline".equals(key)) deadlineEditText.setText(value);
            else if ("Genre".equals(key)) genreEditText.setText(value);
            else if ("Location".equals(key)) locationEditText.setText(value);
        }
    }

    private void updateEvent() {
        // Checking to see if maxentrant is a valid answer
        Integer listmaxchecker = 1;
        String listmax = maxentrantEditText.getText().toString();
        // Making sure we won't crash if we make it into a positive number
        try {
            Integer.parseInt(listmax);
        } catch (NumberFormatException e) {
            listmaxchecker = 0;
            Toast.makeText(getContext(), "Enter in a proper Max Enterant Amount", Toast.LENGTH_SHORT).show();
        }
        // This will continue as long as the previous checks passed
        if (listmaxchecker == 1 && Integer.parseInt(listmax) >= 0) {
            Map<String, Object> updatedEventData = new HashMap<>();

            String Name = eventNameEditText.getText().toString();
            String MaxPeople = maxPeopleEditText.getText().toString();
            String Date = dateEditText.getText().toString();
            String Deadline = deadlineEditText.getText().toString();
            String Genre = genreEditText.getText().toString();
            if (!locationEditText.getText().toString().isEmpty()) {
                String Location = locationEditText.getText().toString();
                updatedEventData.put("Location", Location);
            }
            if (maxentrantEditText.getText().toString().isEmpty() == false) {
                String maxentrant = maxentrantEditText.getText().toString();
                updatedEventData.put("Wait List Maximum", maxentrant);
            }
            // --- Update in Firebase ---
            updatedEventData.put("Name", Name);
            updatedEventData.put("Max People", MaxPeople);
            updatedEventData.put("Date", Date);
            updatedEventData.put("Deadline", Deadline);
            updatedEventData.put("Genre", Genre);


            db.collection("Events").document(originalEventId)
                    .update(updatedEventData) // or .update()
                    .addOnSuccessListener(aVoid -> {
                        // Navigate back
                        if (!Name.isEmpty() && !MaxPeople.isEmpty() && !Date.isEmpty() && !Deadline.isEmpty()
                                && !Genre.isEmpty()) {
                            NavHostFragment.findNavController(this).navigateUp();
                            Toast.makeText(getContext(), "Event updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating event", Toast.LENGTH_SHORT).show());
        }
        else{
            // In case the Max Enterant amount is negetive :)
            Toast.makeText(getContext(), "Enter in a proper Max Enterant Amount", Toast.LENGTH_SHORT).show();
        }
    }
}
