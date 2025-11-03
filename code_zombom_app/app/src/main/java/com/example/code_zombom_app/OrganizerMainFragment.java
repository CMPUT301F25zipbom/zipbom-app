package com.example.code_zombom_app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;


public class OrganizerMainFragment extends Fragment {

    private EventViewModel eventViewModel;
    private LinearLayout eventsContainer;// <-- Changed from TextView
    //private EventAdapter eventAdapter;
    private FirebaseFirestore db;
    private CollectionReference eventsdb;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the same shared ViewModel instance
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_organizer_main, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //eventDisplayTextView = view.findViewById(R.id.event_display_textview);

        // View setup
        super.onViewCreated(view, savedInstanceState);

        eventsContainer = view.findViewById(R.id.events_container_linearlayout);

        // Find the add event button
        Button addButton = view.findViewById(R.id.add_event_button);

        // Set the click listener to navigate to the next fragment.
        addButton.setOnClickListener(v -> {
            // Use NavController to navigate to the AddEventFragment.
            NavHostFragment.findNavController(OrganizerMainFragment.this)
                    .navigate(R.id.action_organizerMainFragment_to_addEventFragment);
        });
        // Observe data
        eventViewModel.getEventList().observe(getViewLifecycleOwner(), events -> {
            updateUiWithEvents(events);
        });
    }

    private void updateUiWithEvents(List<String> events) {
        // First, remove all previous TextViews to avoid duplicates
        eventsContainer.removeAllViews();

        db = FirebaseFirestore.getInstance();
        eventsdb = db.collection("Events");
        eventsdb.addSnapshotListener((value, error) ->{
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()){
                // Clearing the list and then looping through all of the events in the database and adding them to the list
                eventsContainer.removeAllViews();
                for (QueryDocumentSnapshot snapshot : value){
                    StringBuilder eventTextView = new StringBuilder();
                    eventTextView.append("Name: ").append(snapshot.getString("Name")).append("\n");
                    eventTextView.append("Max People: ").append(snapshot.getString("Max People")).append("\n");
                    eventTextView.append("Date: ").append(snapshot.getString("Date")).append("\n");
                    eventTextView.append("Deadline: ").append(snapshot.getString("Deadline")).append("\n");
                    eventTextView.append("Genre: ").append(snapshot.getString("Genre")).append("\n");
                    if (snapshot.getString("Location") != null){
                        eventTextView.append("Location: ").append(snapshot.getString("Location"));
                    }

                    // Adding the special effects to the eventdisplay. We also convert the StringBuilder to a TextView
                    String texttodisplay = eventTextView.toString();
                    TextView eventdisplay = new TextView(getContext());
                    eventdisplay.setText(texttodisplay);
                    eventdisplay.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                    eventdisplay.setTextSize(18);
                    eventdisplay.setPadding(16, 16, 16, 16);
                    eventsContainer.addView(eventdisplay);
                }
            }
            else{
                // If the list is empty, show a "No events" message
                TextView noEventsTextView = new TextView(getContext());
                noEventsTextView.setText("No events yet.");
                noEventsTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                noEventsTextView.setTextSize(18);
                noEventsTextView.setPadding(16, 16, 16, 16);
                eventsContainer.addView(noEventsTextView);
            }
        });

    }
    // In OrganizerMainFragment.java

    private void showEventOptionsDialog(String eventText) {
        // Create a new dialog
        final Dialog dialog = new Dialog(getContext());
        // We don't want a title bar
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set the custom layout for the dialog
        dialog.setContentView(R.layout.dialog_event_options);

        // Find the buttons inside the dialog's layout
        Button viewStartButton = dialog.findViewById(R.id.button_start_draw);
        Button messageButton = dialog.findViewById(R.id.button_message_participants);
        Button editEventButton = dialog.findViewById(R.id.button_edit_event);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);

        // Set click listeners for each button
        viewStartButton.setOnClickListener(v -> {
            // TODO: Implement Draw
            Toast.makeText(getContext(), "Viewing details for the event...", Toast.LENGTH_SHORT).show();
            dialog.dismiss(); // Close the dialog
        });

        messageButton.setOnClickListener(v -> {
            // TODO: Implement Message Entrants
            Toast.makeText(getContext(), "Editing event...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        editEventButton.setOnClickListener(v -> {
            // TODO: Implement Editing
            Toast.makeText(getContext(), "Sharing event...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            // This should just get rid of the pop up
            dialog.dismiss();
        });

        // Make the dialog's background transparent
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Show the dialog
        dialog.show();
    }

}
