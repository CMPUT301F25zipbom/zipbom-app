package com.example.code_zombom_app.organizer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.system.Os;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.ContentResolver;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;
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
    protected EventService eventService;
    /** Canonical event instance retained when editing to keep lists intact. */
    protected Event baseEvent;
    protected Uri imageUri;
    private Uri pendingImageUri = null; // To handle the race condition
    //private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> resultLauncher;

    /**
     * Abstract method that subclasses must implement to define what happens when the "Save" or "Update" button is clicked.
     * @param event The canonical event being saved or updated.
     */
    protected abstract void processEvent(Event event);
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        eventService = new EventService(db);

        registerResult();

        // Initialize the image picker launcher
//        imagePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        // Take persistable permission to Os.access the image URI across restarts
//                        Uri newlySelectedUri = result.getData().getData();
//                        if (newlySelectedUri != null) {
//                            try {
//                                // Assign the URI to the class variable so it can be used later
//                                imageUri = newlySelectedUri;
//
//                                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
//
//                                // Get the ContentResolver and take persistable permission
//                                ContentResolver resolver = requireActivity().getContentResolver();
//                                resolver.takePersistableUriPermission(imageUri, takeFlags); // This now uses the correct, non-null URI
//
//                                // Now that we have permission, set the image preview
//                                imagePreview.setImageURI(imageUri);
//                                imagePreview.setVisibility(View.VISIBLE);
//
//                            } catch (SecurityException e) {
//                                // This can happen if the user selects an image from a source that doesn't support
//                                // persistable permissions. Log the error and inform the user.
//                                e.printStackTrace();
//                                Toast.makeText(getContext(), "Could not get permission for the selected image.", Toast.LENGTH_SHORT).show();
//                            }
//                        }
////                        imageUri = result.getData().getData();
////                        imagePreview.setImageURI(imageUri);
////                        imagePreview.setVisibility(View.VISIBLE);
//                    }
//                }
//        );
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

        if (pendingImageUri != null) {
            imagePreview.setImageURI(pendingImageUri);
            imagePreview.setVisibility(View.VISIBLE);
            pendingImageUri = null; // Clear it so it doesn't run again
        }
        Button cancelButton = view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> navigateBack());

        buttonUploadPhoto.setOnClickListener(v -> openGallery());
    }

    /**
     * Opens the device's gallery for the user to pick an image.
     */
    private void openGallery() {
//        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
//        resultLauncher.launch(intent);

//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("image/*"); // Specify that we only want to see image files
//
//        imagePickerLauncher.launch(intent);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*"); // Specify that we only want to see image files

        resultLauncher.launch(intent);
    }
    public void registerResult(){
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri newlySelectedUri = result.getData().getData();
                        if (newlySelectedUri != null) {
                            try {
                                ContentResolver resolver = requireActivity().getContentResolver();
                                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                resolver.takePersistableUriPermission(newlySelectedUri, takeFlags);

                                // This is now the permanent URI we will use for uploads
                                imageUri = newlySelectedUri;

                                // --- THIS IS THE KEY FIX ---
                                // Check if the view is created. If not, store the URI to be set later.
                                if (imagePreview != null) {
                                    // View is ready, set the image directly
                                    imagePreview.setImageURI(imageUri);
                                    imagePreview.setVisibility(View.VISIBLE);
                                } else {
                                    // View is not ready, store the URI in our temporary variable
                                    pendingImageUri = imageUri;
                                }

                            } catch (Exception e) {
                                Log.e("ImagePicker", "Failed to take persistable permission", e);
                                Toast.makeText(getContext(), "Could not get permission for selected image.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult o) {
//                        try{
//                            imageUri = o.getData().getData();
//                            imagePreview.setImageURI(imageUri);
//                            imagePreview.setVisibility(View.VISIBLE);
//                        } catch (Exception e){
//                            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
        );
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

        // --- REFACTORED: Create or update the canonical Event object ---
        Event event = gatherEventData(eventId);
        if (event == null) {
            Toast.makeText(getContext(), "Invalid event details. Please check your input.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageAndProcessEvent(event);
        } else {
            // No new image, just process the event object.
            processEvent(event);
        }
    }

    private void uploadImageAndProcessEvent(Event event) {
        StorageReference storageRef = storage.getReference().child("posters/" + event.getFirestoreDocumentId() + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // --- REFACTORED: Set the poster URL on the event object ---
                            event.setPosterUrl(uri.toString());
                            processEvent(event); // Process the fully updated event
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to get poster URL.", Toast.LENGTH_SHORT).show();
                            processEvent(event); // Process without the poster URL
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Poster upload failed.", Toast.LENGTH_SHORT).show();
                    processEvent(event); // Process without the poster URL
                });
    }


    /**
     * Gathers all data from the EditText fields into an Event object. When editing,
     * the baseEvent (loaded from Firestore) is mutated to preserve waitlist and lottery lists.
     * @return A populated Event object or null when input is invalid.
     */
    private Event gatherEventData(String eventId) {
        String name = eventNameEditText.getText().toString();
        Event event;
        try {
            event = (baseEvent != null) ? baseEvent : new Event(name);
            event.setName(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        event.setFirestoreDocumentId(eventId);
        event.setEventDate(dateEditText.getText().toString());
        event.setRegistrationClosesAt(deadlineEditText.getText().toString());
        event.setLocation(locationEditText.getText().toString());
        event.setDescription(descriptionEditText.getText().toString());
        // Poster URL is set later when an image is uploaded

        // Replace any previous genre with the current input
        event.clearCategories();
        String genre = genreEditText.getText().toString();
        if (!genre.trim().isEmpty()) {
            try {
                event.addCategory(genre);
            } catch (IllegalArgumentException ignored) {
                Toast.makeText(getContext(), "Genre not in the Categories.", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        try {
            int capacity = Integer.parseInt(maxPeopleEditText.getText().toString());
            event.setCapacity(capacity);
        } catch (NumberFormatException ignored) {
            event.setCapacity(0);
        }

        try {
            int waitLimit = Integer.parseInt(maxentrantEditText.getText().toString());
            event.setWaitlistLimit(waitLimit);
        } catch (NumberFormatException ignored) {
            event.setWaitlistLimit(0);
        }

        return event;
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
