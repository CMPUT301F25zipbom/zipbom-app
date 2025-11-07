package com.example.code_zombom_app.organizer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This class is responsible for creating a new event, making sure the event is valid and saving it to firebase
 */
public class AddEventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private EditText eventNameEditText;
    private EditText maxPeopleEditText;
    private EditText dateEditText;
    private EditText deadlineEditText;
    private EditText genreEditText;
    private EditText locationEditText;
    private EditText maxentrantEditText;
    private EditText descriptionEditText;
    private FirebaseFirestore db;
    private CollectionReference events;
    private Button saveEventButton;
    private Button cancelButton;
    private Button buttonUploadPhoto;
    private ImageView imagePreview;
    private Uri imageUri; // To hold the URI of the selected image
    private ActivityResultLauncher<Intent> imagePickerLauncher;


    /**
     * We get the new view model in this method
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state. Also Initialize the
     * image picker launcher to get a image for the poster.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Scoped to the Activity, so it's shared between fragments.
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);

        // Initialize the launcher to get the result from the gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Image was selected successfully
                        imageUri = result.getData().getData();
                        imagePreview.setImageURI(imageUri);
                        imagePreview.setVisibility(View.VISIBLE); // Show the preview
                    }
                }
        );
    }

    /**
     * Inflates the layout.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_event, container, false);
    }

    /**
     * This method gets the data from the user and creates a new event if data is valid.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        events = db.collection("Events");

        cancelButton = view.findViewById(R.id.cancelButton);
        buttonUploadPhoto = view.findViewById(R.id.buttonUploadPhoto);

        imagePreview = view.findViewById(R.id.imagePreview);

        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        dateEditText = view.findViewById(R.id.editTextDate);
        deadlineEditText = view.findViewById(R.id.editTextDeadline);
        genreEditText = view.findViewById(R.id.editTextGenre);
        locationEditText = view.findViewById(R.id.editTextLocation);
        maxentrantEditText = view.findViewById(R.id.maxamountofentrants);
        descriptionEditText = view.findViewById(R.id.description);

        saveEventButton = view.findViewById(R.id.saveEventButton);

        cancelButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
        });

        buttonUploadPhoto.setOnClickListener(v -> openGallery());

        saveEventButton.setOnClickListener(v -> {
            String eventName = eventNameEditText.getText().toString();

            if (!eventName.isEmpty() && !maxPeopleEditText.getText().toString().isEmpty()
                    && !dateEditText.getText().toString().isEmpty() && !deadlineEditText.getText().toString().isEmpty()
                    && !genreEditText.getText().toString().isEmpty()
                    && maxentrantchecker(maxentrantEditText.getText().toString()) && validdate(dateEditText.getText().toString(), deadlineEditText.getText().toString())) {
                //just for the UI visuals
                String name = eventNameEditText.getText().toString();
                String maxPeople = maxPeopleEditText.getText().toString();
                String date = dateEditText.getText().toString();
                String deadline = deadlineEditText.getText().toString();
                String genre = genreEditText.getText().toString();
                String location = locationEditText.getText().toString();
                String listmax = maxentrantEditText.getText().toString();
                String description = descriptionEditText.getText().toString();

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("Name", name);
                eventData.put("Max People", maxPeople);
                eventData.put("Date", date);
                eventData.put("Deadline", deadline);
                eventData.put("Genre", genre);
                if (!location.isEmpty()) {
                    eventData.put("Location", location);
                }
                if (listmax.isEmpty() == false) {
                    eventData.put("Wait List Maximum", listmax);
                }
                eventData.put("Entrants", new ArrayList<Entrant>());
                eventData.put("Cancelled Entrants", new ArrayList<Entrant>());
                eventData.put("Accepted Entrants", new ArrayList<Entrant>());
                eventData.put("Lottery Winners", new ArrayList<Entrant>());
                if (description.isEmpty() == false) {
                    eventData.put("Description", description);
                }
                db.collection("Events").add(eventData);

                String newEventId = events.getId();
                uploadImageAndUpdateEvent(newEventId);

                // Navigate back to the main fragment
                NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
            }

        });
    }

    // This function is used so check if the dates are valid. If they are not, then we return false.

    /**
     * @param date1 Consists of a string MMM DD YYYY (example Jan 6 2025)
     * @param date2 Consists of a string MMM DD YYYY
     * @return returns the true if date1 is after date 2. Else, it returns false and a message why
     */
    boolean validdate (String date1, String date2){
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
        String[] validmonths = {"jan","feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        if (!Arrays.asList(validmonths).contains(eventdate[0].toLowerCase()) || !Arrays.asList(validmonths).contains(deadlinedate[0].toLowerCase())){
            Toast.makeText(getContext(), "Invalid Month format.", Toast.LENGTH_SHORT).show();
            return isvalid;
        }
        //Check to make sure the deadline is before the event.
        // Start with the event year, then check the month, then finally check the date. If dates are equal then its invalid.
        if (Integer.parseInt(eventdate[2]) < Integer.parseInt(deadlinedate[2])){
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
     *
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
     *
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveEvent() {
        // ... (gather all your data from EditText fields into a Map<String, Object> eventData)
        Map<String, Object> eventData = new HashMap<>();
        String eventName = eventNameEditText.getText().toString();
        // ... get all other fields ...

        if (eventName.isEmpty()) {
            Toast.makeText(getContext(), "Event Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        eventData.put("Name", eventName);
        eventData.put("Entrants", new ArrayList<String>());
        eventData.put("Accepted Entrants", new ArrayList<String>());
        eventData.put("Cancelled Entrants", new ArrayList<String>());

        // Create the event document in Firestore to get a unique ID
        db.collection("Events").add(eventData)
                .addOnSuccessListener(documentReference -> {
                    String newEventId = documentReference.getId();
                    Log.d("Firestore", "Event document created with ID: " + newEventId);

                    // Check if an image was selected
                    if (imageUri != null) {
                        // If yes, upload the image using the new event ID
                        uploadImageAndUpdateEvent(newEventId);
                    } else {
                        // If no image, we are done. Just navigate back.
                        Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error creating event", e);
                    Toast.makeText(getContext(), "Failed to create event.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     *
     * @param eventId
     */
    private void uploadImageAndUpdateEvent(String eventId) {
        // Create a path in Firebase Storage: "posters/event_id.jpg"
        com.google.firebase.storage.StorageReference storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child("posters/" + eventId + ".jpg");

        // Upload the file
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the public download URL for the uploaded image
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();

                        // Update the event document with the poster URL
                        db.collection("Events").document(eventId)
                                .update("posterUrl", imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Event updated with poster URL.");
                                    Toast.makeText(getContext(), "Event and poster saved!", Toast.LENGTH_SHORT).show();
                                    NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("Firestore", "Failed to update event with poster URL", e);
                                    // Even if this fails, the event is created, so navigate back
                                    NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w("Storage", "Image upload failed", e);
                    Toast.makeText(getContext(), "Event saved, but poster upload failed.", Toast.LENGTH_SHORT).show();
                    // Navigate back anyway, as the main data was saved
                    NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
                });
    }

}