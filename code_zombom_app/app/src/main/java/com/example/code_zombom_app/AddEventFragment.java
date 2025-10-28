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

import com.example.code_zombom_app.EventViewModel;
import com.example.code_zombom_app.R;

public class AddEventFragment extends Fragment {

    private EventViewModel eventViewModel;private EditText eventNameEditText;
    private Button saveButton;

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

        eventNameEditText = view.findViewById(R.id.editTextText2); // Use your actual ID
        saveButton = view.findViewById(R.id.saveEventButton); // Use your actual ID

        saveButton.setOnClickListener(v -> {
            String eventName = eventNameEditText.getText().toString();
            if (!eventName.isEmpty()) {
                // Use the ViewModel to add the new event
                eventViewModel.addEvent(eventName);

                // Navigate back to the main fragment
                NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
            }
        });
    }
}