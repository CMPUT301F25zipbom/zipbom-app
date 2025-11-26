package com.example.code_zombom_app.organizer;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Map;

/**
 * @author Tejwinder Johal
 * @version 1.0
 * This class is responsible for making the organizer dialog that
 * offers different options relating to the selected event.
 */
public class OrganizerDialog extends Dialog {
    private final Event event; // <<< Use the Event object
    private final NavController navController;
//    private final View fragmentView;
//    private final Map<String, Bitmap> qrCodeBitmaps;
    private final ImageView qrCodeImageView; // Direct reference to the ImageView
    private final Bitmap qrCodeBitmap;       // The specific bitmap for this event

    private final EventService eventService = new EventService(FirebaseFirestore.getInstance());

    /**
     * This method is used to make an organizerdialog object.
     * @param context sets the context
     * @param event sets the organizerdialog eventid
     * @param navController sets the organizerdialog navController
     * @param qrCodeImageView sets the organizerdialog fragmentView
     */
    public OrganizerDialog(@NonNull Context context, Event event, NavController navController,
                           View fragmentView, Map<String, Bitmap> qrCodeBitmaps) {
    public OrganizerDialog(@NonNull Context context, EventForOrg eventForOrg, NavController navController, ImageView qrCodeImageView, Bitmap qrCodeBitmap) { // Pass ImageView and Bitmap directly
        super(context);
        this.event = event; // <<< Store the whole object
        this.navController = navController;
        this.qrCodeImageView = qrCodeImageView; // Store the direct reference
        this.qrCodeBitmap = qrCodeBitmap;       // Store the specific bitmap
    }

    /**
     * This sets up the different buttons and tells them what to do if they get clicked
     * @param savedInstanceState If this dialog is being reinitialized after a
     *     the hosting activity was previously shut down, holds the result from
     *     the most recent call to {@link #onSaveInstanceState}, or null if this
     *     is the first time.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set the custom layout for the dialog popup
        setContentView(R.layout.dialog_event_options);

        // Find the buttons
        Button viewStartButton = findViewById(R.id.button_start_draw);
        Button messageButton = findViewById(R.id.button_message_participants);
        Button editEventButton = findViewById(R.id.button_edit_event);
        Button genQRButton = findViewById(R.id.genQRButton);
        Button seeDetsButton = findViewById(R.id.seeDetailsButton);
        Button cancelButton = findViewById(R.id.button_cancel);

        if (event.getLotteryWinners() != null && !event.getLotteryWinners().isEmpty()) {
            viewStartButton.setText("Replacement Draw");
        } else {
            viewStartButton.setText("Start Draw");
        }

        // This button starts a draw for who will win the lottery using the central service
        viewStartButton.setOnClickListener(v -> {
            dismiss(); // Close the dialog
            runLottery();
        });
        // This button messages all of the people who have entered or who have won the lottery. NOT SURE WHICH.
        messageButton.setOnClickListener(v -> {
            // TODO: Implement Message Entrants
            dismiss();
        });
        //This will send the user to EditEventFragment along with the event's id and the events text.
        editEventButton.setOnClickListener(v -> {
            dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId()); // Get ID from the object

            // Navigate to the edit fragment
            navController.navigate(R.id.action_organizerMainFragment_to_editEventFragment, bundle);
        });
        //This makes the QR code visible when the user clicks generate QR code
        genQRButton.setOnClickListener(v -> {
            if (qrCodeImageView != null && qrCodeBitmap != null) {
                qrCodeImageView.setImageBitmap(qrCodeBitmap);
                qrCodeImageView.setVisibility(View.VISIBLE);
                eventForOrg.setQrCodeExists(true); // Update the state
                uploadQrCodeToFirebase(qrCodeBitmap);
            } else {
                // 5. If something is wrong, show a clear error.
                Toast.makeText(getContext(), "Error: Could not display QR code.", Toast.LENGTH_SHORT).show();
                Log.e("OrganizerDialog", "QR Bitmap or ImageView was null. Cannot display.");
            dismiss();
            Bitmap qrBitmap = qrCodeBitmaps.get(event.getEventId());
            // We need the fragment's root view to find the tag
            ImageView qrToShow = fragmentView.findViewWithTag(event.getEventId());

            if (qrToShow != null) {
                uploadQrCodeToFirebase(qrBitmap);
                if (qrToShow.getVisibility() == View.GONE) {
                    qrToShow.setVisibility(View.VISIBLE);
                } else {
                    // If for some reason it's not found, show an error
                    Toast.makeText(getContext(), "Error: QR Code image not found.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //This gets rid of the popup.
        seeDetsButton.setOnClickListener(v -> {
            dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId()); // Get ID from the object    // Navigate to the full details fragment

            // Navigate to the full details fragment
            navController.navigate(R.id.action_organizerMainFragment_to_eventFullDetailsFragment, bundle);
        });

        cancelButton.setOnClickListener(v -> dismiss());

        // Make the dialog's background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
    /**
     * Uploads the provided QR code Bitmap to Firebase Storage and saves the URL to Firestore.
     * @param bitmap The QR code bitmap to upload.
     */
    private void uploadQrCodeToFirebase(Bitmap bitmap) {
        Toast.makeText(getContext(), "Saving QR Code...", Toast.LENGTH_SHORT).show();

        // Convert Bitmap to byte array for upload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        // Define the path in Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("qr_codes/" + event.getEventId() + ".png");

        // Upload the byte array
        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the public download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Save the URL to the event's document in Firestore
                        saveQrUrlToFirestore(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload to firebase failed. Please try again.", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseStorage", "QR code upload to firebase failed", e);
                });
    }
    /**
     * Saves the QR code's download URL to a 'qrCodeUrl' field in the event's document.
     * @param url The public URL of the uploaded image.
     */
    private void saveQrUrlToFirestore(String url) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get the reference to the specific event document
        db.collection("Events").document(eventForOrg.getEventId())
                .update(
                        "qrCodeUrl", url,          // Save the URL
                        "qrCodeExists", true   // --- THIS IS THE CRITICAL FIX ---
                )
        FirebaseFirestore.getInstance().collection("Events").document(event.getEventId())
                .update("qrCodeUrl", url)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "QR Code Saved Successfully!", Toast.LENGTH_LONG).show();
                    Log.d("Firestore", "QR Code URL and exists flag updated for event: " + eventForOrg.getEventId());
                    Log.d("Firestore", "QR Code URL updated for event: " + event.getEventId());
                    dismiss(); // Close the dialog on success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save QR code details.", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error updating event with QR details", e);
                    dismiss();
                });
    }

    /**
     * Runs a lottery draw via the central EventService and surfaces the outcome to the organiser.
     */
    private void runLottery() {
        eventService.runLotteryDraw(event.getEventId())
                .addOnSuccessListener(ignored -> Toast.makeText(getContext(), "Lottery draw completed.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), e.getMessage() != null ? e.getMessage() : "Lottery draw failed.", Toast.LENGTH_SHORT).show();
                    Log.e("OrganizerDialog", "Lottery draw failed", e);
                });
    }
}
