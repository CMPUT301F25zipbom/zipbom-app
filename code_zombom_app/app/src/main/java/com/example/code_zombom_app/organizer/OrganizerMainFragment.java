package com.example.code_zombom_app.organizer;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.Login.LoginActivity;
import com.example.code_zombom_app.MainActivity;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This is the class that sets up the main organizer ui
 */
public class OrganizerMainFragment extends Fragment {

    private EventViewModel eventViewModel;
    private LinearLayout eventsContainer;// <-- Changed from TextView
    //private EventAdapter eventAdapter;
    private FirebaseFirestore db;
    private CollectionReference eventsdb;

    // This map will store the generated QR code bitmaps with the eventId as the key.
    private Map<String, Bitmap> qrCodeBitmaps = new HashMap<>();
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private EventForOrg eventToExport; // Temporarily store the event that needs exporting


    /**
     * This function gets the model and saves it to eventViewModel
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission was granted, now we can export.
                if (eventToExport != null) {
                    exportEntrantsToCsv(eventToExport);
                }
            } else {
                // Permission was denied.
                Toast.makeText(getContext(), "Storage permission is required to save the CSV file.", Toast.LENGTH_LONG).show();
            }
        });
        // Get the same shared ViewModel instance
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    /**
     * Gets the inflated view of the main organizer and returns it.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return This function returns the inflated view of the 'main' ui page for the organizer
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_organizer_main, container, false);
        return view;
    }

    /**
     * This function links the buttons to their respective variables. It also sets up firebase and calls setupFirestoreListener()
     * to fill the organizer ui with wuth all of the events from the database.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //eventDisplayTextView = view.findViewById(R.id.event_display_textview);

        // View setup
        super.onViewCreated(view, savedInstanceState);

        eventsContainer = view.findViewById(R.id.events_container_linearlayout);
        db = FirebaseFirestore.getInstance();
        eventsdb = db.collection("Events");

        setupFirestoreListener();
        // Find the add event button
        Button addButton = view.findViewById(R.id.button_organizer_main_fragment_add_event);

        // Set the click listener to navigate to the next fragment.
        addButton.setOnClickListener(v -> {
            // Use NavController to navigate to the AddEventFragment.
            NavHostFragment.findNavController(OrganizerMainFragment.this)
                    .navigate(R.id.action_organizerMainFragment_to_addEventFragment);
        });

        Button buttonLogOut = view.findViewById(R.id.button_organizer_main_fragment_logout);
        buttonLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent main = new Intent(requireActivity(), MainActivity.class);
                main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(main);
                requireActivity().finish();
            }
        });
    }


    /**
     * This function is used to fill the main organizer ui with all of the events from the database.
     */
    private void setupFirestoreListener() {
        eventsdb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return; // Stop execution if there's an error
            }
            // Always clear the container before adding new views
            eventsContainer.removeAllViews();
            qrCodeBitmaps.clear();

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    try {
                        // --- NEW: Automatically convert the document to an Event object ---
                        Event event = snapshot.toObject(Event.class);
                        // If toObject returns null, something is wrong with the data mapping (e.g., field name mismatch)
                        if (event == null) {
                            Log.e("DATA_MAPPING_ERROR", "Event object is null for document: "
                                    + snapshot.getId() +
                                    ". Check Firestore fields against the organizer.Event class.");
                            continue; // Skip this document and move to the next
                        }

                        // --- GET THE EVENT ID AND BUILD THE TEXT ---
                        View eventItemView = LayoutInflater.from(getContext()).inflate(
                                R.layout.event_list_item, eventsContainer, false);
                        TextView eventDetailsTextView = eventItemView.findViewById(
                                R.id.textView_event_list_items_details);
                        ImageView qrCodeImageView = eventItemView.findViewById(
                                R.id.event_qr_code_imageview);

                        TextView textViewName = eventItemView.findViewById(
                                R.id.textView_event_list_item_name);
                        TextView textViewGenre = eventItemView.findViewById(
                                R.id.textView_event_list_item_genre);
                        TextView textViewStartDate = eventItemView.findViewById(
                                R.id.textView_event_list_item_startDate);
                        TextView textViewEndDate = eventItemView.findViewById(
                                R.id.textView_event_list_item_endDate);
                        TextView textViewLocation = eventItemView.findViewById(
                                R.id.textView_event_list_item_location);

                        qrCodeImageView.setTag(event.getEventId());

                        // --- Use the convenience method from the Event class ---
                        String eventText = event.getDescription();
                        eventDetailsTextView.setText("Details: " + eventText);
                        textViewName.setText("Name: " + event.getName());
                        textViewGenre.setText("Genre: " + event.getGenre());
                        if (event.getEventStartDate() != null)
                            textViewStartDate.setText("Start Date: " +
                                    event.getEventStartDate().toString());
                        if (event.getEventEndDate() != null)
                            textViewEndDate.setText("End Date: " + event.getEventEndDate());
                        if (event.getLocation() != null)
                            textViewLocation.setText("Location: " + event.getLocation().toString());

                        qrCodeImageView.setImageBitmap(event.getEventIdBitmap());

                        // --- Set click listener (pass the object or its properties) ---
                        eventItemView.setOnClickListener(v -> showEventOptionsDialog(event));
                        eventsContainer.addView(eventItemView);
                        EventForOrg eventForOrg = EventMapper.toDto(event);
                        if (eventForOrg.getQrCodeExists()) {
                            qrCodeImageView.setVisibility(View.VISIBLE);
                        }

                    }  catch (Exception e) {
                        // This will catch NullPointerExceptions if a view ID is wrong
                        Log.e("DATA_MAPPING_ERROR",
                                "Error converting document to Event object. " +
                                        "Check Firestore field names!", e);
                    }
                }
            } else {
                // If there are no documents, show a "No events" message
                TextView noEventsTextView = new TextView(getContext());
                noEventsTextView.setText("No events yet.");
                noEventsTextView.setTextColor(ContextCompat.getColor(
                        getContext(), android.R.color.white));
                noEventsTextView.setTextSize(18);
                noEventsTextView.setPadding(16, 16, 16, 16);
                eventsContainer.addView(noEventsTextView);
            }
        });
    }

    /**
     * Makes the Organizer Dialog pop-up.
     * @param event The event that the user clicked on
     */
    private void showEventOptionsDialog(Event event) {
        NavController navController = NavHostFragment.findNavController(this);
        View fragmentView = getView();

        EventForOrg eventForOrg = EventMapper.toDto(event);
        // Get the specific bitmap for this event from the map.
        Bitmap qrBitmapForEvent = qrCodeBitmaps.get(eventForOrg.getEventId());

        // Find the specific ImageView using the tag
        // We search within the fragment's main view.
        ImageView qrImageViewForEvent = null;
        if (fragmentView != null) {
            qrImageViewForEvent = fragmentView.findViewWithTag(eventForOrg.getEventId());
        }

        // This is a "task" we will give to the dialog.
        Runnable requestExportPermissionTask = () -> {
            this.eventToExport = eventForOrg; // Store the event we're working on
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Only launch if permission is actually needed.
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                // For modern Android, no permission is needed, just export.
                exportEntrantsToCsv(eventForOrg);
            }
        };
        // Create the dialog with the direct references.
        // This now matches the new constructor you will create in OrganizerDialog.
        OrganizerDialog dialog = new OrganizerDialog(
                requireContext(),
                eventForOrg,
                event,
                navController,
                qrImageViewForEvent,
                qrBitmapForEvent,
                requestExportPermissionTask // <-- Pass the task to the dialog
        );

        dialog.show();
    }

    private void exportEntrantsToCsv(EventForOrg event) {
        // This is the exact same logic from your dialog, now moved here.
        ArrayList<String> entrants = event.getAccepted_Entrants();

        if (entrants == null || entrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "entrants_" + event.getName().replaceAll("[^a-zA-Z0-9_.]", "_") + ".csv";
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Accepted Entrants\n");
        for (String entrantId : entrants) {
            csvContent.append(entrantId).append("\n");
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");

        Uri uri = requireContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        if (uri != null) {
            try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(csvContent.toString().getBytes());
                    Toast.makeText(getContext(), "Exported to Downloads folder!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("CSV_EXPORT", "Error writing to file", e);
                Toast.makeText(getContext(), "Failed to export file.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Failed to create file in Downloads.", Toast.LENGTH_SHORT).show();
        }
    }
}
