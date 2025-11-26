package com.example.code_zombom_app.organizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.example.code_zombom_app.organizer.EventForOrg;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;

import kotlinx.coroutines.scheduling.Task;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 2.0
 * This class is used when the user wants to edit an event.
 */
public class EditEventFragment extends BaseEventFragment {
    private String originalEventId;
    private EventForOrg eventForOrgToEdit;
    private Event domainEventToEdit;
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
                    if (documentSnapshot.exists() && isAdded() && getView() != null){
                        // Convert document to Event object
                        eventForOrgToEdit = documentSnapshot.toObject(EventForOrg.class);
                        if (eventForOrgToEdit == null) return;
                        domainEventToEdit = EventMapper.toDomain(eventForOrgToEdit, documentSnapshot.getId());
                        baseEvent = domainEventToEdit;

                        // Set the ID on the object after loading it
                        eventForOrgToEdit.setEventId(documentSnapshot.getId());

                        // --- Populate all fields from the object ---
                        eventNameEditText.setText(eventForOrgToEdit.getName());
                        maxPeopleEditText.setText(eventForOrgToEdit.getMax_People());
                        dateEditText.setText(eventForOrgToEdit.getDate());
                        deadlineEditText.setText(eventForOrgToEdit.getDeadline());
                        genreEditText.setText(eventForOrgToEdit.getGenre());
                        locationEditText.setText(eventForOrgToEdit.getLocation());
                        descriptionEditText.setText(eventForOrgToEdit.getDescription());
                        maxentrantEditText.setText(eventForOrgToEdit.getWait_List_Maximum());

                        if (eventForOrgToEdit.getPosterUrl() != null && !eventForOrgToEdit.getPosterUrl().isEmpty()) {

                            // Add a null check on imagePreview before using it.
                            if (imagePreview != null) {
                                Glide.with(requireContext())
                                        .load(eventForOrgToEdit.getPosterUrl())
                                        .into(imagePreview);

                                imagePreview.setVisibility(View.VISIBLE);
                                isPosterUploaded = true; // Make sure your flag is set
                            }
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
     * @param eventFromUi The complete Event object with updated data.
     */
    @Override
    protected void processEvent(Event eventFromUi) {
        // If the original event failed to load, we can't safely proceed.
        if (domainEventToEdit == null) {
            Toast.makeText(getContext(), "Error: Original event not loaded. Cannot save.", Toast.LENGTH_LONG).show();
            if(updateButton != null) {
                updateButton.setEnabled(true);
            }
            return;
        }

        // Merge editable fields into the canonical event while keeping waitlist/lottery data.
        domainEventToEdit.setName(eventFromUi.getName());
        domainEventToEdit.setCapacity(eventFromUi.getCapacity());
        domainEventToEdit.setEventDate(eventFromUi.getEventDateText());
        domainEventToEdit.setRegistrationClosesAt(eventFromUi.getRegistrationClosesAtText());
        domainEventToEdit.setLocation(eventFromUi.getLocation());
        domainEventToEdit.setGenre(eventFromUi.getGenre());
        domainEventToEdit.setDescription(eventFromUi.getDescription());
        domainEventToEdit.setMaxEntrants(eventFromUi.getMaxEntrants());
        domainEventToEdit.setPosterUrl(eventFromUi.getPosterUrl());
        domainEventToEdit.setFirestoreDocumentId(originalEventId);

        // Disable button immediately on click to prevent double-taps
        updateButton.setEnabled(false);

        eventService.saveEvent(domainEventToEdit)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event updated successfully", Toast.LENGTH_SHORT).show();

                    // Now we need to notify the users that the event they have joined in has changed
                    notifyusers(eventFromUi);

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

    void notifyusers (Event ourevent){
        // We need to loop through the list of people and see if they have notifications turned on.
        // Then we check to see if they have a phone number, then we SMS. If not, then we only email.

        // Get this list of entrants and then we loop through
        ArrayList<String> people = ourevent.getWaitingList();
        for (int i = 0; i < people.size(); i++){
            boolean notifications = true;
            String phonenum = "stupid"; // Need to access user profile or whatever to fill these two vars
            int i2 = i;
            db.collection("Profiles").document(people.get(i)).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (notifications == true){
                            sendemail(people.get(i2));
                            if (phonenum.length() == 9){
                                try {
                                    int testcase = Integer.parseInt(phonenum);
                                    sendsmsmessage(phonenum);
                                } catch (NumberFormatException ignored){}
                            }
                        }
                    });

        }
    }

    void sendemail(String useremail){

        String[] to = {useremail};
        String[] cc = {"zipbomapp@gmail.com"};

        Intent mail = new Intent(Intent.ACTION_SEND);
        mail.setData(Uri.parse("mailto:"));
        mail.putExtra(Intent.EXTRA_EMAIL, to);
        mail.putExtra(Intent.EXTRA_CC, cc);
        mail.putExtra(Intent.EXTRA_TEXT, "The event you have signed up for has been edited.");
        mail.putExtra(Intent.EXTRA_SUBJECT, "Zipbomapp event edited");

        startActivity(Intent.createChooser(mail, "mailto"));

        //Need to save a notification to the database

    }

    void sendsmsmessage(String phonenumber){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phonenumber));
        intent.putExtra("sms_body", "An event you have signed up for has been edited.");

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "SMS message fail :(", Toast.LENGTH_SHORT).show();
        }
        // Need to save a notification to the database
        
    }
}
