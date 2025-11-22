package com.example.code_zombom_app.organizer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;

/**
 * An abstract base class for fragments that add or edit events.
 * It handles all shared UI components, input validation, and image handling logic.
 */
public abstract class BaseEventFragment extends Fragment {

    // Common UI Components
    protected EditText eventNameEditText, maxPeopleEditText, dateEditText, deadlineEditText,
            genreEditText, locationEditText, maxentrantEditText, descriptionEditText;
    protected Button buttonUploadPhoto;
    protected ImageView imagePreview;

    // Common variables
    protected FirebaseFirestore db;
    protected FirebaseStorage storage;
    protected Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    /**
     * Abstract method that subclasses must implement to define what happens when the "Save" or "Update" button is clicked.
     * @param eventForOrg The ID of the event (can be new or existing).
     */
    protected abstract void processEvent(EventForOrg eventForOrg);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize the image picker launcher
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Use a common layout for both add and edit fragments
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUI(view);
    }

    /**
     * Initializes all UI components from the view.
     */
    private void initializeUI(View view) {
        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        dateEditText = view.findViewById(R.id.editTextDate);
        deadlineEditText = view.findViewById(R.id.editTextDeadline);
        genreEditText = view.findViewById(R.id.editTextGenre);
        locationEditText = view.findViewById(R.id.editTextLocation);
        maxentrantEditText = view.findViewById(R.id.maxamountofentrants);
        descriptionEditText = view.findViewById(R.id.editTextDescription);
        buttonUploadPhoto = view.findViewById(R.id.buttonUploadPhoto2);
        imagePreview = view.findViewById(R.id.imagePreview);

        Button cancelButton = view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> navigateBack());

        buttonUploadPhoto.setOnClickListener(v -> openGallery());
    }

    /**
     * Opens the device's gallery for the user to pick an image.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Main action method triggered by the primary button (Save/Update).
     * It validates input, gathers data, and starts the image upload process if needed.
     */
    /**
     * Main action method. It now creates an Event object instead of a Map.
     */
    protected void onSaveOrUpdateButtonClicked(String eventId) {
        if (!validateAllInput()) {
            return; // Validation methods show Toasts.
        }

        // --- REFACTORED: Create an Event object instead of a Map ---
        EventForOrg eventForOrg = gatherEventData();
        eventForOrg.setEventId(eventId); // Set the ID for the new or existing event

        if (imageUri != null) {
            uploadImageAndProcessEvent(eventForOrg);
        } else {
            // No new image, just process the event object.
            processEvent(eventForOrg);
        }
    }

    private void uploadImageAndProcessEvent(EventForOrg eventForOrg) {
        StorageReference storageRef = storage.getReference().child("posters/" + eventForOrg.getEventId() + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // --- REFACTORED: Set the poster URL on the event object ---
                            eventForOrg.setPosterUrl(uri.toString());
                            processEvent(eventForOrg); // Process the fully updated event
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to get poster URL.", Toast.LENGTH_SHORT).show();
                            processEvent(eventForOrg); // Process without the poster URL
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Poster upload failed.", Toast.LENGTH_SHORT).show();
                    processEvent(eventForOrg); // Process without the poster URL
                });
    }


    /**
     * REFACTORED: Gathers all data from the EditText fields into an Event object.
     * @return A new Event object populated with UI data.
     */
    private EventForOrg gatherEventData() {
        EventForOrg eventForOrg = new EventForOrg();
        eventForOrg.setName(eventNameEditText.getText().toString());
        eventForOrg.setMax_People(maxPeopleEditText.getText().toString()); // Use the new field name
        eventForOrg.setDate(dateEditText.getText().toString());
        eventForOrg.setDeadline(deadlineEditText.getText().toString());
        eventForOrg.setGenre(genreEditText.getText().toString());
        eventForOrg.setLocation(locationEditText.getText().toString());
        eventForOrg.setWait_List_Maximum(maxentrantEditText.getText().toString()); // Use the new field name
        eventForOrg.setDescription(descriptionEditText.getText().toString());
        return eventForOrg;
    }

    protected void navigateBack() {
        if (isAdded()) {
            NavHostFragment.findNavController(this).navigateUp();
        }
    }

    // region Input Validation
    private boolean validateAllInput() {
        String name = eventNameEditText.getText().toString();
        String maxPeople = maxPeopleEditText.getText().toString();
        String date = dateEditText.getText().toString();
        String deadline = deadlineEditText.getText().toString();
        String genre = genreEditText.getText().toString();

        if (name.isEmpty() || maxPeople.isEmpty() || date.isEmpty() || deadline.isEmpty() || genre.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!maxentrantchecker(maxentrantEditText.getText().toString()) ||
                !validdatechecker(date, deadline)) {
            return false;
        }
        return true;
    }

    public boolean maxentrantchecker(String listmax) {
        if (listmax.isEmpty()) return true;
        try {
            if (Integer.parseInt(listmax) < 0) {
                Toast.makeText(getContext(), "Max entrants must be a positive number.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format for max entrants.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validdatechecker(String date1, String date2) {
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
        if (Integer.parseInt(eventdate[1]) > 31 || Integer.parseInt(eventdate[1]) > 31){
            Toast.makeText(getContext(), "Days beyond 31 not allowed.", Toast.LENGTH_SHORT).show();
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
    // endregion
}
