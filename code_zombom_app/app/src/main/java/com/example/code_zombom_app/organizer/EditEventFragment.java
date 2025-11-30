package com.example.code_zombom_app.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.Helpers.Mail.Mail;
import com.example.code_zombom_app.Helpers.Mail.MailService;
import com.example.code_zombom_app.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.example.code_zombom_app.organizer.EventForOrg;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;

import kotlinx.coroutines.scheduling.Task;

/**
 * Fragment that allows an organizer to edit an existing Event.
 *
 * @author Dang Nguyen, Teji
 * @version 11/27/2025
 */
public class EditEventFragment extends AddEventFragment {

    private static final String TAG = "EditEventFragment";

    private String eventId;
    private Event baseEvent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the event ID passed into this fragment
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button saveButton = view.findViewById(R.id.saveEventButton);
        saveButton.setText("Update Event");
        saveButton.setOnClickListener(v -> onSaveOrUpdateButtonClicked());

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event ID provided for editing.",
                    Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    baseEvent = snapshot.toObject(Event.class);
                    if (baseEvent == null) {
                        Toast.makeText(getContext(),
                                "Failed to load event for editing.",
                                Toast.LENGTH_SHORT).show();
                        navigateBack();
                        return;
                    }

                    // Ensure the ID is set on the object if it's not already
                    try {
                        if (baseEvent.getEventId() == null ||
                                baseEvent.getEventId().isEmpty()) {
                            baseEvent.setEventId(eventId);
                        }
                    } catch (Exception ignored) {}

                    bindEventToForm(baseEvent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event", e);
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Error loading event for editing.",
                            Toast.LENGTH_SHORT).show();
                    navigateBack();
                });
    }

    /**
     * Override the base "save" handler so that we mutate and upload the already-loaded event,
     * instead of constructing a brand new one.
     */
    @Override
    protected void onSaveOrUpdateButtonClicked() {
        if (!validateAllInput()) {
            return;
        }

        if (baseEvent == null) {
            Toast.makeText(getContext(),
                    "Event not loaded yet, please wait a moment.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Update baseEvent's fields from the form
        updateBaseEventFromForm();

        // If the user picked a new image, upload it and then save
        if (imageUri != null) {
            uploadImageAndProcessEvent(baseEvent);
        } else {
            // Reuse the parent's processEvent() which calls eventModel.uploadEvent(...)
            processEvent(baseEvent);
        }
    }

    /**
     * Fill the form fields from the loaded Event.
     *
     * @param event The loaded event
     */
    private void bindEventToForm(@NonNull Event event) {
        // Name, description, counts
        if (event.getName() != null) {
            eventNameEditText.setText(event.getName());
        }

        event.setLocation(event.getLocation());
        descriptionEditText.setText(
                event.getDescription() == null ? "" : event.getDescription()
        );

        try {
            maxPeopleEditText.setText(String.valueOf(event.getCapacity()));
        } catch (Exception ignored) {
            maxPeopleEditText.setText("");
        }

        try {
            maxentrantEditText.setText(String.valueOf(event.getWaitlistLimit()));
        } catch (Exception ignored) {
            maxentrantEditText.setText("");
        }

        // Dates
        Date start = event.getEventStartDate();
        if (start != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            datePickerStartDate.updateDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
        }

        Date end = event.getEventEndDate();
        if (end != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(end);
            datePickerEndDate.updateDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
        }

        // Genre → set spinner selection
        if (event.getGenre() != null && spinnerGenre != null &&
                spinnerGenre.getAdapter() instanceof ArrayAdapter) {

            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerGenre.getAdapter();
            int pos = adapter.getPosition(event.getGenre());
            if (pos >= 0) {
                spinnerGenre.setSelection(pos);
                selectedGenre = event.getGenre();
            }
        }

        Location loc = event.getLocation();
        if (loc != null) {
            location = loc;
            if (autocompleteSupportFragmentEventAddress != null) {
                autocompleteSupportFragmentEventAddress.setText(loc.toString());
            }
        }

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            imagePreview.setVisibility(View.VISIBLE);
            Glide.with(requireContext())
                    .load(event.getPosterUrl())
                    .into(imagePreview);
        } else {
            imagePreview.setVisibility(View.GONE);
        }
    }

    /**
     * Copy values from the form into the loaded baseEvent, without touching its ID / internal lists.
     */
    private void updateBaseEventFromForm() {
        baseEvent.setName(eventNameEditText.getText().toString().trim());
        baseEvent.setDescription(descriptionEditText.getText().toString().trim());
        baseEvent.setGenre(selectedGenre);
        baseEvent.setEventStartDate(getDateFromDatePicker(datePickerStartDate));
        baseEvent.setEventEndDate(getDateFromDatePicker(datePickerEndDate));
        baseEvent.setLocation(location);

        // We notify users
        notifyusers(baseEvent);

        try {
            int capacity = Integer.parseInt(maxPeopleEditText.getText().toString());
            baseEvent.setCapacity(capacity);
        } catch (NumberFormatException e) {
            baseEvent.setCapacity(0);
        }

        try {
            int waitLimit = Integer.parseInt(maxentrantEditText.getText().toString());
            baseEvent.setWaitlistLimit(waitLimit);
        } catch (NumberFormatException e) {
            baseEvent.setWaitlistLimit(0);
        }

        // NOTE: we do NOT touch waitlist entries, chosen list, etc.
    }

    void notifyusers (Event ourevent){
        // We need to loop through the list of people and see if they have notifications turned on.
        // Then we check to see if they have a phone number, then we SMS. If not, then we only email.

        // Get this list of entrants and then we loop through
        ArrayList<String> people = ourevent.getRegisteredList();
        if (people == null){return;}
        for (int i = 0; i < people.size(); i++){
            Mail noti = new Mail(people.get(i), people.get(i), Mail.MailType.EDITED_EVENT);
            noti.setReceiver(people.get(i));
            noti.setContent("An event you have entered into has been edited.");
            noti.setRead(false);
            noti.setHeader("Edited event");
            noti.setTimestamp(Timestamp.now());
            MailService.sendMail(noti);
        }
    }
}
