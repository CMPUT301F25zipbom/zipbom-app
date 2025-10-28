package com.example.code_zombom_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;

public class AddEventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private EditText eventNameEditText;
    private EditText maxPeopleEditText;
    private EditText dateEditText;
    private EditText deadlineEditText;
    private EditText genreEditText;
    private EditText locationEditText;
    private Button saveEventButton;
    // Create list of events
    ArrayList<ArrayList<String>> listOfEvents = new ArrayList<ArrayList<String>>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Scoped to the Activity, so it's shared between fragments.
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_event, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button cancelButton = view.findViewById(R.id.cancelButton);

        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        dateEditText = view.findViewById(R.id.editTextDate);
        deadlineEditText = view.findViewById(R.id.editTextDeadline);
        genreEditText = view.findViewById(R.id.editTextGenre);
        locationEditText = view.findViewById(R.id.editTextLocation);


        saveEventButton = view.findViewById(R.id.saveEventButton);

        cancelButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
        });

        //TODO: add poster and QR code generation
        saveEventButton.setOnClickListener(v -> {
            ArrayList<String> eventInfo = new ArrayList<>();
            eventInfo.add(eventNameEditText.getText().toString());
            eventInfo.add(maxPeopleEditText.getText().toString());
            eventInfo.add(dateEditText.getText().toString());
            eventInfo.add(deadlineEditText.getText().toString());
            eventInfo.add(genreEditText.getText().toString());

            String eventName = eventNameEditText.getText().toString();
            if (!eventName.isEmpty() && !maxPeopleEditText.getText().toString().isEmpty()
                    && !dateEditText.getText().toString().isEmpty() && !deadlineEditText.getText().toString().isEmpty()
                    && !genreEditText.getText().toString().isEmpty()) {
                if (!locationEditText.getText().toString().isEmpty()) {
                    eventInfo.add(locationEditText.getText().toString());
                }
                // Use the ViewModel to add the new event
                //eventViewModel.addEvent(eventName);
                eventViewModel.addEvent(eventInfo.toString()); //just for testing
                listOfEvents.add(eventInfo);

                // Navigate back to the main fragment
                NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
            }
        });
    }
}