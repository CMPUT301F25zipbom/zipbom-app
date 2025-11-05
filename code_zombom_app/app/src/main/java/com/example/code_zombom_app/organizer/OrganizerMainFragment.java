package com.example.code_zombom_app.organizer;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

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
        //setupFirestoreListener();
    }


    private void setupFirestoreListener() {
        eventsdb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return; // Stop execution if there's an error
            }
            // Always clear the container before adding new views
            eventsContainer.removeAllViews();

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    try {
                        // --- GET THE EVENT ID AND BUILD THE TEXT ---
                        String eventId = snapshot.getId(); // <<< GET THE DOCUMENT ID HERE
                        View eventItemView = LayoutInflater.from(getContext()).inflate(R.layout.event_list_item, eventsContainer, false);
                        TextView eventDetailsTextView = eventItemView.findViewById(R.id.event_item_textview);
                        ImageView qrCodeImageView = eventItemView.findViewById(R.id.event_qr_code_imageview);

                        qrCodeImageView.setTag(eventId);

                        String eventNameForQR = snapshot.getString("Name");
                        StringBuilder eventTextBuilder = new StringBuilder();

                        eventTextBuilder.append("Name: ").append(snapshot.getString("Name")).append("\n");
                        eventTextBuilder.append("Max People: ").append(snapshot.getString("Max People")).append("\n");
                        eventTextBuilder.append("Date: ").append(snapshot.getString("Date")).append("\n");
                        eventTextBuilder.append("Deadline: ").append(snapshot.getString("Deadline")).append("\n");
                        eventTextBuilder.append("Genre: ").append(snapshot.getString("Genre")).append("\n");
                        String location = snapshot.getString("Location");
                        if (!TextUtils.isEmpty(location)) {
                            eventTextBuilder.append("Location: ").append(location).append("\n");
                        }
                        String eventText = eventTextBuilder.toString(); // <<< THIS IS THE FULL TEXT

                        eventDetailsTextView.setText(eventText);

                        // Generate and set the QR code
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        // Use the eventName variable as the content for the QR code
                        Bitmap bitmap = barcodeEncoder.encodeBitmap(eventNameForQR, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
                        qrCodeImageView.setImageBitmap(bitmap);

                        // Set click listener for the whole item
                        eventItemView.setOnClickListener(v -> showEventOptionsDialog(eventId, eventText));
                        // Add the finished view to the screen
                        eventsContainer.addView(eventItemView);

                    } catch (WriterException e) {
                        Log.e("QRCode", "Error generating QR code", e);                        // Optionally set a placeholder if the QR code fails to generate
                    } catch (Exception e) {
                        // This will catch NullPointerExceptions if a view ID is wrong
                        Log.e("UI_ERROR", "Error processing event item. Check your XML IDs.", e);
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
    private void showEventOptionsDialog(String eventId, String eventText) {
        NavController navController = NavHostFragment.findNavController(this);
        // We need to pass the fragment's root view so the dialog can find the ImageView tag
        View fragmentView = getView();

        OrganizerDialog dialog = new OrganizerDialog(requireContext(), eventId, eventText, navController, fragmentView);
        dialog.show();
    }
}
