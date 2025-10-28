package com.example.code_zombom_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import androidx.lifecycle.ViewModelProvider;
import android.widget.TextView;

public class OrganizerMainFragment extends Fragment {

    private EventViewModel eventViewModel;
    private TextView eventDisplayTextView; // Example: a TextView to show event names

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the same shared ViewModel instance
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_organizer_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // View setup
        super.onViewCreated(view, savedInstanceState);

        // Find the button
        Button addButton = view.findViewById(R.id.add_event_button);

        // Set the click listener to navigate to the next fragment.
        addButton.setOnClickListener(v -> {
            // Use NavController to navigate to the AddEventFragment.
            NavHostFragment.findNavController(OrganizerMainFragment.this)
                    .navigate(R.id.action_organizerMainFragment_to_addEventFragment);
        });

        // **OBSERVE THE DATA**
        eventViewModel.getEventList().observe(getViewLifecycleOwner(), events -> {
            // This code runs every time the event list changes.
            StringBuilder eventsText = new StringBuilder();
            for (String event : events) {
                eventsText.append(event).append("\n");
            }
            eventDisplayTextView.setText(eventsText.toString());
        });
    }
}