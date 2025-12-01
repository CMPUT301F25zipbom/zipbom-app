package com.example.code_zombom_app.organizer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.Helpers.Models.EventModel;
import com.example.code_zombom_app.Helpers.Location.Coordinate;
import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.google.android.gms.common.api.Status;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This class is responsible for creating a new event, making sure the event is valid and saving it to firebase
 */
public class AddEventFragment extends Fragment {

    // Common UI Components
    protected EditText eventNameEditText, maxPeopleEditText,
            maxentrantEditText, descriptionEditText;

    protected View startDateCard;
    protected View endDateCard;
    protected TextView startDatePrimaryText;
    protected TextView startDateSecondaryText;
    protected TextView endDatePrimaryText;
    protected TextView endDateSecondaryText;

    protected AutocompleteSupportFragment autocompleteSupportFragmentEventAddress;
    protected Spinner spinnerGenre;

    protected Button buttonUploadPhoto;
    protected ImageView imagePreview;

    // Common variables
    protected FirebaseFirestore db;
    protected FirebaseStorage storage;
    protected Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    protected Location location = null; // Selected location

    protected Event event = new Event();
    protected String selectedGenre = "";

    protected EventModel eventModel = new EventModel();

    protected Calendar startCalendar;
    protected Calendar endCalendar;
    private final SimpleDateFormat cardPrimaryFormat =
            new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
    private final SimpleDateFormat cardSecondaryFormat =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

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

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().getApplicationContext(), Location.getGoogleApi());
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Use a common layout for both add and edit fragments
        return inflater.inflate(R.layout.fragment_add_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button saveButton = view.findViewById(R.id.saveEventButton);

        saveButton.setOnClickListener(v -> {
            // Create a new empty document to get a unique ID
            DocumentReference newEventRef = db.collection("Events").document();

            // Call the shared save/update handler from the base class
            onSaveOrUpdateButtonClicked();
        });

        initializeUI(view);
    }

    /**
     * Initializes all UI components from the view.
     */
    private void initializeUI(View view) {
        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        startDateCard = view.findViewById(R.id.card_start_date);
        endDateCard = view.findViewById(R.id.card_end_date);
        startDatePrimaryText = view.findViewById(R.id.textStartDatePrimary);
        startDateSecondaryText = view.findViewById(R.id.textStartDateSecondary);
        endDatePrimaryText = view.findViewById(R.id.textEndDatePrimary);
        endDateSecondaryText = view.findViewById(R.id.textEndDateSecondary);
        maxentrantEditText = view.findViewById(R.id.maxamountofentrants);
        descriptionEditText = view.findViewById(R.id.editTextDescription);
        buttonUploadPhoto = view.findViewById(R.id.buttonUploadPhoto);
        imagePreview = view.findViewById(R.id.imagePreview);

        Button cancelButton = view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> navigateBack());

        buttonUploadPhoto.setOnClickListener(v -> openGallery());

        // Initialize default start/end dates (today and tomorrow)
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.DAY_OF_MONTH, 1);
        updateDateDisplays();

        startDateCard.setOnClickListener(v -> showDatePicker(true));
        endDateCard.setOnClickListener(v -> showDatePicker(false));

        autocompleteSupportFragmentEventAddress = (AutocompleteSupportFragment)
        getChildFragmentManager().findFragmentById(R.id.fragment_autoComplete_event_address);

        assert autocompleteSupportFragmentEventAddress != null;
        autocompleteSupportFragmentEventAddress.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG));
        autocompleteSupportFragmentEventAddress.setHint("Enter your event address");
        autocompleteSupportFragmentEventAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.e("Address", "Error: " + status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                new Thread(() ->{
                    if (place.getLatLng() != null) {
                        Coordinate coordinate = new Coordinate(
                                place.getLatLng().latitude,
                                place.getLatLng().longitude
                        );

                        location = Location.fromCoordinates(coordinate);
                        if (place.getName() != null && !place.getName().trim().isEmpty()) {
                            assert location != null;
                            location.setName(place.getName());
                        }
                    } else {
                        Log.w("Location", "No Place selected!");
                    }
                }).start();
            }
        });

        String[] acceptedGenres = Event.getAcceptedCategories();

        spinnerGenre = view.findViewById(R.id.spinnerGenre);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                view.getContext(),
                android.R.layout.simple_spinner_item,
                acceptedGenres
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerGenre.setAdapter(adapter);

        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGenre = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGenre = parent.getItemAtPosition(0).toString();
            }
        });
    }

    /**
     * Refreshes the date cards with the current calendar selections.
     */
    protected void updateDateDisplays() {
        if (startDatePrimaryText != null && startCalendar != null) {
            startDatePrimaryText.setText(cardPrimaryFormat.format(startCalendar.getTime()));
        }
        if (startDateSecondaryText != null && startCalendar != null) {
            startDateSecondaryText.setText(cardSecondaryFormat.format(startCalendar.getTime()));
        }
        if (endDatePrimaryText != null && endCalendar != null) {
            endDatePrimaryText.setText(cardPrimaryFormat.format(endCalendar.getTime()));
        }
        if (endDateSecondaryText != null && endCalendar != null) {
            endDateSecondaryText.setText(cardSecondaryFormat.format(endCalendar.getTime()));
        }
    }

    private void showDatePicker(boolean isStart) {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        if (isStart) {
            long today = Calendar.getInstance().getTimeInMillis();
            constraintsBuilder.setStart(today);
            constraintsBuilder.setValidator(DateValidatorPointForward.from(today));
            builder.setTitleText("Select start date");
            builder.setSelection(startCalendar.getTimeInMillis());
        } else {
            Calendar minEnd = (Calendar) startCalendar.clone();
            minEnd.add(Calendar.DAY_OF_MONTH, 1);
            long minEndMillis = minEnd.getTimeInMillis();
            constraintsBuilder.setStart(minEndMillis);
            constraintsBuilder.setValidator(DateValidatorPointForward.from(minEndMillis));
            builder.setTitleText("Select end date");
            long defaultSelection = Math.max(endCalendar.getTimeInMillis(), minEndMillis);
            builder.setSelection(defaultSelection);
        }
        builder.setCalendarConstraints(constraintsBuilder.build());
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            int year = utc.get(Calendar.YEAR);
            int month = utc.get(Calendar.MONTH);
            int day = utc.get(Calendar.DAY_OF_MONTH);

            Calendar chosen = Calendar.getInstance();
            chosen.set(Calendar.YEAR, year);
            chosen.set(Calendar.MONTH, month);
            chosen.set(Calendar.DAY_OF_MONTH, day);
            chosen.set(Calendar.HOUR_OF_DAY, 0);
            chosen.set(Calendar.MINUTE, 0);
            chosen.set(Calendar.SECOND, 0);
            chosen.set(Calendar.MILLISECOND, 0);

            if (isStart) {
                startCalendar = chosen;
                Calendar minEnd = (Calendar) startCalendar.clone();
                minEnd.add(Calendar.DAY_OF_MONTH, 1);
                if (endCalendar.before(minEnd)) {
                    endCalendar = (Calendar) minEnd.clone();
                }
            } else {
                endCalendar = chosen;
            }
            updateDateDisplays();
        });
        picker.show(getChildFragmentManager(), isStart ? "start_date_picker" : "end_date_picker");
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
    protected void onSaveOrUpdateButtonClicked() {
        if (!validateAllInput()) {
            return; // Validation methods show Toasts.
        }

        // --- REFACTORED: Create or update the canonical Event object ---
        Event event = gatherEventData();
        if (event == null) {
            Toast.makeText(getContext(), "Invalid event details. Please check your input.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageAndProcessEvent(event);
        } else {
            // No new image, just process the event object.
            processEvent(event);
        }
    }

    protected void uploadImageAndProcessEvent(Event event) {
        StorageReference storageRef = storage.getReference().child("posters/" +
                event.getEventId() + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // --- REFACTORED: Set the poster URL on the event object ---
                            event.setPosterUrl(uri.toString());
                            processEvent(event); // Process the fully updated event
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to get poster URL.",
                                    Toast.LENGTH_SHORT).show();
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
    protected Event gatherEventData() {
        String name = eventNameEditText.getText().toString();
        try {
            event = new Event(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        event.setEventStartDate(getDateFromCalendar(startCalendar));
        event.setEventEndDate(getDateFromCalendar(endCalendar));
        event.setLocation(location);
        event.setDescription(descriptionEditText.getText().toString());
        event.setGenre(selectedGenre);
        // Poster URL is set later when an image is uploaded

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
    protected boolean validateAllInput() {
        String name = eventNameEditText.getText().toString();
        String maxPeople = maxPeopleEditText.getText().toString();

        if (name.isEmpty() || maxPeople.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields.", Toast.LENGTH_SHORT).show();
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

    // endregion

    /**
     * Helper to convert a Calendar instance to a Date (null-safe).
     */
    protected Date getDateFromCalendar(@Nullable Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        Calendar clone = (Calendar) calendar.clone();
        clone.set(Calendar.HOUR_OF_DAY, 0);
        clone.set(Calendar.MINUTE, 0);
        clone.set(Calendar.SECOND, 0);
        clone.set(Calendar.MILLISECOND, 0);
        return clone.getTime();
    }

    /**
     * REFACTORED: Implements the abstract method to create a new Firestore document
     * directly from the Event object.
     * @param event The complete Event object to be saved.
     */
    protected void processEvent(Event event) {
        // Use the central EventService to persist with the canonical schema
        eventModel.uploadEvent(event);

        // TODO: Handle the case when uploading event failed. For now it mostly succeed
        navigateBack();
    }
}
