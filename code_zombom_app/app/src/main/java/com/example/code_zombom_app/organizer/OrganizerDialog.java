package com.example.code_zombom_app.organizer;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ProgressBar;
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

import com.bumptech.glide.Glide;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.io.ByteArrayOutputStream;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.Map;

/**
 * @author Tejwinder Johal
 * @version 1.0
 * Briefly explain what this does Mr. Johal.
 */
public class OrganizerDialog extends Dialog {
    private final String eventId;
    private final String eventText;
    private final NavController navController;
    private final View fragmentView;
    private final Map<String, Bitmap> qrCodeBitmaps;

    /**
     * This method is used to make an organizerdialog object.
     * @param context sets the context
     * @param eventId sets the organizerdialog eventid
     * @param eventText sets the organizerdialog eventtext
     * @param navController sets the organizerdialog navController
     * @param fragmentView sets the organizerdialog fragmentView
     */
    public OrganizerDialog(@NonNull Context context, String eventId, String eventText, NavController navController, View fragmentView, Map<String, Bitmap> qrCodeBitmaps) {
        super(context);
        this.eventId = eventId;
        this.eventText = eventText;
        this.navController = navController;
        this.fragmentView = fragmentView;
        this.qrCodeBitmaps = qrCodeBitmaps;
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

        // This button starts a draw for who will win the lottery
        viewStartButton.setOnClickListener(v -> {
            // TODO: Implement Draw
            dismiss(); // Close the dialog
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
            bundle.putString("eventId", eventId);
            bundle.putString("eventText", eventText);

            // Navigate to the edit fragment
            navController.navigate(R.id.action_organizerMainFragment_to_editEventFragment, bundle);
        });
        //This makes the QR code visible when the user clicks generate QR code
        genQRButton.setOnClickListener(v -> {
            dismiss();
            Bitmap qrBitmap = qrCodeBitmaps.get(eventId);
            // We need the fragment's root view to find the tag
            ImageView qrToShow = fragmentView.findViewWithTag(eventId);
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
            bundle.putString("eventId", eventId);

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

        // 1. Convert Bitmap to byte array for upload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        // 2. Define the path in Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("qr_codes/" + eventId + ".png");

        // 3. Upload the byte array
        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // 4. Get the public download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // 5. Save the URL to the event's document in Firestore
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
        FirebaseFirestore.getInstance().collection("Events").document(eventId)
                .update("qrCodeUrl", url)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "QR Code Saved Successfully!", Toast.LENGTH_LONG).show();
                    Log.d("Firestore", "QR Code URL updated for event: " + eventId);
                    dismiss(); // Close the dialog on success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save QR code URL.", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error updating event with QR URL", e);
                });
    }
}
