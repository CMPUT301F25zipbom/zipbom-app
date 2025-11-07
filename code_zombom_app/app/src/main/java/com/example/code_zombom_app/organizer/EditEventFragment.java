package com.example.code_zombom_app.organizer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This class is used when the user wants to edit an event.
 */
public class EditEventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private FirebaseFirestore db;
    private String originalEventId;
    private String originalEventText; // The full text of the event
    private CollectionReference eventref;
    private FirebaseFirestore db1;
    private CollectionReference events;
    private Button buttonUploadPhoto;
    private ImageView imagePreview;
    private Uri imageUri;
    private String existingPosterUrl;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private EditText eventNameEditText, maxPeopleEditText, dateEditText, deadlineEditText, genreEditText, locationEditText, maxentrantEditText, descriptionEditText;

    /**
     * This sets up the eventViewModel, database and catches the arguments.
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.  Also Initialize the
     * image picker launcher to get a image for the poster.
     */
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

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imagePreview.setImageURI(imageUri);
                        imagePreview.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    /**
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Returns the inflated edit fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
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

        db1 = FirebaseFirestore.getInstance();
        events = db1.collection("Events");

        eventref = db1.collection("Events");


        Button cancelButton = view.findViewById(R.id.cancelButton);

        buttonUploadPhoto = view.findViewById(R.id.buttonUploadPhoto2);
        imagePreview = view.findViewById(R.id.imagePreview);

        buttonUploadPhoto.setOnClickListener(v -> openGallery());

        // Find all EditTexts
        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        dateEditText = view.findViewById(R.id.editTextDate);
        deadlineEditText = view.findViewById(R.id.editTextDeadline);
        genreEditText = view.findViewById(R.id.editTextGenre);
        locationEditText = view.findViewById(R.id.editTextLocation);
        maxentrantEditText = view.findViewById(R.id.maxamountofentrants);
        descriptionEditText = view.findViewById(R.id.editTextDescription);


        // Pre-fill the fields with existing data
        if (originalEventId != null) {
            loadEventData();
        }
        populateFields();

        cancelButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(EditEventFragment.this).navigateUp();
        });

        Button updateButton = view.findViewById(R.id.saveEventButton);
        updateButton.setOnClickListener(v -> updateEvent());
    }

    /**
     * This function gets all of the previous events info and autofills out the textboxes.
     */
    private void populateFields() {
        // A more robust solution would be a proper data model.
        if (originalEventText == null) return;

        // Setting Wait List Maximum text
        if (originalEventId != null) {
            db.collection("Events").document(originalEventId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            if (documentSnapshot.contains("Description")) {
                                String description = documentSnapshot.getString("Description");
                                descriptionEditText.setText(description);
                            }
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

    /**
     * This function checks to make sure maxentrant is properly set up and that the dates are properly set up
     * Then it gets all of the stuff inside of the textboxes and updates the database.
     */
    private void updateEvent() {
        if (!maxentrantchecker(maxentrantEditText.getText().toString()) ||
                !validdatechecker(dateEditText.getText().toString(), deadlineEditText.getText().toString())) {
            // The helper methods already show a Toast message, so we just exit.
            return;
        }

        // 2. Gather all data from the UI into a Map.
        Map<String, Object> updatedEventData = new HashMap<>();
        String name = eventNameEditText.getText().toString();
        String maxPeople = maxPeopleEditText.getText().toString();
        String date = dateEditText.getText().toString();
        String deadline = deadlineEditText.getText().toString();
        String genre = genreEditText.getText().toString();
        String location = locationEditText.getText().toString();
        String maxEntrant = maxentrantEditText.getText().toString();
        String description = descriptionEditText.getText().toString();

        // Basic field validation
        if (name.isEmpty() || maxPeople.isEmpty() || date.isEmpty() || deadline.isEmpty() || genre.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        updatedEventData.put("Name", name);
        updatedEventData.put("Max People", maxPeople);
        updatedEventData.put("Date", date);
        updatedEventData.put("Deadline", deadline);
        updatedEventData.put("Genre", genre);
        updatedEventData.put("Location", location);
        updatedEventData.put("Wait List Maximum", maxEntrant);
        updatedEventData.put("Description", description);


        // 3. Decide how to proceed based on whether a new image was selected.
        if (imageUri != null) {
            // --- CASE 1: A new poster was selected. Upload it first. ---
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("posters/" + originalEventId + ".jpg");
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Image upload successful, now get the download URL.
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Add the new image URL to our data map.
                            updatedEventData.put("posterUrl", uri.toString());
                            // Now, make ONE call to update Firestore with all data.
                            updateFirestoreDocument(updatedEventData);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Poster upload failed. Saving other changes.", Toast.LENGTH_SHORT).show();
                        // Still update the text fields even if image upload fails.
                        updateFirestoreDocument(updatedEventData);
                    });
        } else {
            // --- CASE 2: No new poster was selected. Just update the text fields. ---
            updateFirestoreDocument(updatedEventData);
        }
    }

    /**
     * Helper method to perform the final Firestore update and navigate back.
     * This avoids duplicating code.
     */
    private void updateFirestoreDocument(Map<String, Object> data) {
        // Note: This does not include the array data (Entrants, etc.). You should add logic
        // from the previous answer to preserve those arrays if needed.
        db.collection("Events").document(originalEventId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event updated successfully", Toast.LENGTH_SHORT).show();
                    if (isAdded()) {
                        NavHostFragment.findNavController(this).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update event.", Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * This is a copy paste of the code from AddEventFragment
     * @param listmax Contains a String that represents a positive number.
     * @return Will return True if the string is a positive number, else, it will give an error message and return false
     */
    boolean maxentrantchecker (String listmax){
        if (listmax.isEmpty()) {
            return true;
        }
        try {
            Integer.parseInt(listmax);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Enter in a proper Max Enterant Amount", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Integer.parseInt(listmax) < 0){
            Toast.makeText(getContext(), "Enter in a positive Max Enterant Amount", Toast.LENGTH_SHORT).show();
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * This is the same code from AddEventFragment. Serves same purpose.
     * @param date1 Consists of a string MMM DD YYYY (example Jan 6 2025)
     * @param date2 Consists of a string MMM DD YYYY
     * @return returns the true if date1 is after date 2. Else, it returns false and a message why
     */
    boolean validdatechecker (String date1, String date2) {
        boolean isvalid = false;
        // Splitting up the different parts of the date.
        String[] eventdate = date1.split(" ");
        String[] deadlinedate = date2.split(" ");

        if (eventdate.length < 3 || deadlinedate.length < 3) {
            Toast.makeText(getContext(), "Please use a valid format.", Toast.LENGTH_SHORT).show();
            return isvalid;
        }
        // Making sure years are valid
        try {
            Integer.parseInt(eventdate[2]);
            Integer.parseInt(deadlinedate[2]);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid year format.", Toast.LENGTH_SHORT).show();
            return isvalid;
        }
        // Making sure days are valid
        try {
            Integer.parseInt(eventdate[1]);
            Integer.parseInt(deadlinedate[1]);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid day format.", Toast.LENGTH_SHORT).show();
            return isvalid;
        }
        // Making sure months are valid
        String[] validmonths = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        if (!Arrays.asList(validmonths).contains(eventdate[0].toLowerCase()) || !Arrays.asList(validmonths).contains(deadlinedate[0].toLowerCase())) {
            Toast.makeText(getContext(), "Invalid Month format.", Toast.LENGTH_SHORT).show();
            return isvalid;
        }
        //Check to make sure the deadline is before the event.
        // Start with the event year, then check the month, then finally check the date. If dates are equal then its invalid.
        if (Integer.parseInt(eventdate[2]) < Integer.parseInt(deadlinedate[2])) {
            Toast.makeText(getContext(), "Invalid Years.", Toast.LENGTH_SHORT).show();
            return isvalid;
        }
        // Testing the case in which years are equal
        if (Integer.parseInt(eventdate[2]) == Integer.parseInt(deadlinedate[2])) {
            if (Arrays.asList(validmonths).indexOf(eventdate[0].toLowerCase()) < Arrays.asList(validmonths).indexOf(deadlinedate[0].toLowerCase())) {
                Toast.makeText(getContext(), "Invalid Month.", Toast.LENGTH_SHORT).show();
                return isvalid;
            }
        }
        // Checking the case if years and months are equal
        if (Integer.parseInt(eventdate[2]) == Integer.parseInt(deadlinedate[2])) {
            if (Arrays.asList(validmonths).indexOf(eventdate[0].toLowerCase()) == Arrays.asList(validmonths).indexOf(deadlinedate[0].toLowerCase())) {
                if (Integer.parseInt(eventdate[1]) <= Integer.parseInt(deadlinedate[1])){
                    Toast.makeText(getContext(), "Invalid Days.", Toast.LENGTH_SHORT).show();
                    return isvalid;
                }
            }
        }

        isvalid = true;
        return isvalid;
    }

    /**
     * Sends a message to a user letting them know that we changed the event.
     * @param user Gives us the user we want to send a message to
     */
    void sendeditedmessage (String user){
        if (originalEventId != null) {
            db.collection("Profiles").document(user).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            if (documentSnapshot.contains("NotificationsPreferences")) {
                                // Check if the field exists in the document
                                String choice = documentSnapshot.getString("NotificationsPreferences");
                                if (choice.isEmpty()) {
                                    Toast.makeText(getContext(), "Entrant has no way to contact", Toast.LENGTH_SHORT).show();
                                }
                                if (choice.equals("email")) {
                                    sendemailmessage(user);
                                }
                                if (choice.equals("sms")) {
                                    sendsmsmessage(user);
                                }
                                if (choice.equals("both")) {
                                    sendemailmessage(user);
                                    sendsmsmessage(user);
                                }
                            }
                            else{
                                Toast.makeText(getContext(), "Entrant hasn't set up their preferences", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Send email to a specific user
     * @param user Contains the user id so we can retrieve from firebase
     */
    void sendemailmessage (String user){
        // Send email
    }

    /**
     * Send an sms message to a specific user
     * @param user Contrains the user id so we can retrieve from firebase
     */
    void sendsmsmessage (String user){
        // Send sms message
    }
    private void loadEventData() {
        db.collection("Events").document(originalEventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // ... (populate all your EditText fields from the document)

                        // Check for an existing poster and display it
                        existingPosterUrl = doc.getString("posterUrl");
                        if (existingPosterUrl != null && !existingPosterUrl.isEmpty()) {
                            imagePreview.setVisibility(View.VISIBLE);
                            Glide.with(this)
                                    .load(existingPosterUrl)
                                    .into(imagePreview);
                        }
                    }
                });
    }
    private void openGallery() {
        // Same as in AddEventFragment
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

}
