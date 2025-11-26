package com.example.code_zombom_app.organizer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

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


    /**
     * This function gets the model and saves it to eventViewModel
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Button addButton = view.findViewById(R.id.add_event_button);

        // Set the click listener to navigate to the next fragment.
        addButton.setOnClickListener(v -> {
            // Use NavController to navigate to the AddEventFragment.
            NavHostFragment.findNavController(OrganizerMainFragment.this)
                    .navigate(R.id.action_organizerMainFragment_to_addEventFragment);
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
                        EventForOrg eventForOrg = snapshot.toObject(EventForOrg.class);
                        // If toObject returns null, something is wrong with the data mapping (e.g., field name mismatch)
                        if (eventForOrg == null) {
                            Log.e("DATA_MAPPING_ERROR", "Event object is null for document: " + snapshot.getId() + ". Check Firestore fields against the organizer.Event class.");
                            continue; // Skip this document and move to the next
                        }
                        eventForOrg.setEventId(snapshot.getId()); // Manually set the document ID
                        Event mappedEvent = EventMapper.toDomain(eventForOrg, snapshot.getId());
                        if (mappedEvent == null) {
                            Log.e("DATA_MAPPING_ERROR", "Mapped event is null for document: " + snapshot.getId());
                            continue;
                        }

                        // --- GET THE EVENT ID AND BUILD THE TEXT ---
                        // String eventId = snapshot.getId(); // <<< GET THE DOCUMENT ID HERE
                        View eventItemView = LayoutInflater.from(getContext()).inflate(R.layout.event_list_item, eventsContainer, false);
                        TextView eventDetailsTextView = eventItemView.findViewById(R.id.event_item_textview);
                        ImageView qrCodeImageView = eventItemView.findViewById(R.id.event_qr_code_imageview);

                        qrCodeImageView.setTag(eventForOrg.getEventId());

                        // --- Use the convenience method from the Event class ---
                        String eventText = eventForOrg.getEventListDisplayText();
                        eventDetailsTextView.setText(eventText);


                        // --- Generate QR Code Data using the canonical mapper ---
                        String qrCodeData = EventMapper.buildQrPayload(mappedEvent, eventForOrg.getPosterUrl());

                        // Generate and set the QR code
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        // Use the eventName variable as the content for the QR code
                        Bitmap bitmap = barcodeEncoder.encodeBitmap(qrCodeData, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
                        qrCodeImageView.setImageBitmap(bitmap);

                        // Store the generated bitmap in our map
                        qrCodeBitmaps.put(eventForOrg.getEventId(), bitmap);

                        // --- Set click listener (pass the object or its properties) ---
                        eventItemView.setOnClickListener(v -> showEventOptionsDialog(eventForOrg));
                        eventsContainer.addView(eventItemView);
                        if (eventForOrg.getQrCodeExists()) {
                            qrCodeImageView.setVisibility(View.VISIBLE);
                        }

                    } catch (WriterException e) {
                        Log.e("QRCode", "Error generating QR code", e);

                    } catch (Exception e) {
                        // This will catch NullPointerExceptions if a view ID is wrong
                        Log.e("DATA_MAPPING_ERROR", "Error converting document to Event object. Check Firestore field names!", e);
                    }
                }
            }else {
                // If there are no documents, show a "No events" message
                TextView noEventsTextView = new TextView(getContext());
                noEventsTextView.setText("No events yet.");
                noEventsTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                noEventsTextView.setTextSize(18);
                noEventsTextView.setPadding(16, 16, 16, 16);
                eventsContainer.addView(noEventsTextView);
            }
        });
    }

    /**
     * Makes the Organizer Dialog pop-up.
     * @param eventForOrg The event that the user clicked on
     */
    private void showEventOptionsDialog(EventForOrg eventForOrg) {
        NavController navController = NavHostFragment.findNavController(this);
        View fragmentView = getView();

        // Get the specific bitmap for this event from the map.
        Bitmap qrBitmapForEvent = qrCodeBitmaps.get(eventForOrg.getEventId());

        // Find the specific ImageView using the tag
        // We search within the fragment's main view.
        ImageView qrImageViewForEvent = null;
        if (fragmentView != null) {
            qrImageViewForEvent = fragmentView.findViewWithTag(eventForOrg.getEventId());
        }

        // Create the dialog with the direct references.
        // This now matches the new constructor you will create in OrganizerDialog.
        OrganizerDialog dialog = new OrganizerDialog(
                requireContext(),
                eventForOrg,
                navController,
                qrImageViewForEvent, // Pass the specific ImageView
                qrBitmapForEvent     // Pass the specific Bitmap
        );

        dialog.show();
    }
}
