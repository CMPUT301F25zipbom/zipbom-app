package com.example.code_zombom_app.organizer;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.Helpers.Location.EventHeatMapActivity;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.OutputStream;
import java.util.ArrayList;

/**
 * @author Tejwinder Johal
 * @version 1.0
 * This class is responsible for making the organizer dialog that
 * offers different options relating to the selected event.
 */
public class OrganizerDialog extends Dialog {
    private final EventForOrg eventForOrg;
    private final Event event;
    private final NavController navController;
    //    private final View fragmentView;
//    private final Map<String, Bitmap> qrCodeBitmaps;
    private final ImageView qrCodeImageView; // Direct reference to the ImageView
    private final Bitmap qrCodeBitmap;       // The specific bitmap for this event
    private final Runnable requestExportPermissionTask; // <-- Add this property

    private final EventService eventService = new EventService(FirebaseFirestore.getInstance());

    /**
     * This method is used to make an organizerdialog object.
     * @param context sets the context
     * @param eventForOrg sets the organizerdialog eventid
     * @param event The Event object. It is here because there is no good way to map EventForOrg
     *              -> Event
     * @param navController sets the organizerdialog navController
     * @param qrCodeImageView sets the organizerdialog fragmentView
     */
    public OrganizerDialog(@NonNull Context context, EventForOrg eventForOrg, Event event,
                           NavController navController, ImageView qrCodeImageView,
                           Bitmap qrCodeBitmap,
                           Runnable requestExportPermissionTask) { // Pass ImageView and Bitmap directly
        super(context);
        this.eventForOrg = eventForOrg;
        this.event = event;
        this.navController = navController;
        this.qrCodeImageView = qrCodeImageView; // Store the direct reference
        this.qrCodeBitmap = qrCodeBitmap;       // Store the specific bitmap
        this.requestExportPermissionTask = requestExportPermissionTask; // <-- Store the task
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
        setContentView(R.layout.organizer_dialog_event_options);

        // Find the buttons
        Button viewStartButton = findViewById(R.id.button_start_draw);
        Button messageButton = findViewById(R.id.button_message_participants);
        Button cancelUnregisteredButton = findViewById(R.id.button_cancel_unregistered);
        Button editEventButton = findViewById(R.id.button_edit_event);
        Button seeDetsButton = findViewById(R.id.seeDetailsButton);
        Button buttonHeatMap = findViewById(R.id.button_dialog_event_options_showLocationHeatMap);
        Button cancelButton = findViewById(R.id.button_cancel);
        Button exportButton = findViewById(R.id.exportCSVButton);

        if (eventForOrg.getLottery_Winners() != null && !eventForOrg.getLottery_Winners().isEmpty()) {
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
            showBroadcastOptions();
        });
        // This button moves all accepted entrants into the canceled entrants list.
        cancelUnregisteredButton.setOnClickListener(v -> {
            dismiss();
            eventService.cancelUnregisteredEntrants(eventForOrg.getEventId());
        });

        //This will send the user to EditEventFragment along with the event's id and the events text.
        editEventButton.setOnClickListener(v -> {
            dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventForOrg.getEventId()); // Get ID from the object

            // Navigate to the edit fragment
            navController.navigate(R.id.action_organizerMainFragment_to_editEventFragment, bundle);
        });

        //This gets rid of the popup.
        seeDetsButton.setOnClickListener(v -> {
            dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventForOrg.getEventId()); // Get ID from the object    // Navigate to the full details fragment

            // Navigate to the full details fragment
            navController.navigate(
                    R.id.action_organizerMainFragment_to_eventFullDetailsFragment, bundle);
        });

        buttonHeatMap.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EventHeatMapActivity.class);
            intent.putExtra(EventHeatMapActivity.EXTRA_EVENT_ID, event.getEventId());
            getContext().startActivity(intent);
            dismiss();
        });

        exportButton.setOnClickListener(v -> {
            requestExportPermissionTask.run();
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());

        // Make the dialog's background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /**
     * Runs a lottery draw via the central EventService and surfaces the outcome to the organiser.
     */
    private void runLottery() {
        eventService.runLotteryDraw(eventForOrg.getEventId())
                .addOnSuccessListener(ignored -> Toast.makeText(getContext(),
                        "Lottery draw completed.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            e.getMessage() != null ? e.getMessage() : "Lottery draw failed.",
                            Toast.LENGTH_SHORT).show();
                    Log.e("OrganizerDialog", "Lottery draw failed", e);
        });
    }

    private void showBroadcastOptions() {
        final CharSequence[] options = {"Waitlist", "Selected", "Cancelled"};
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Send update to:")
                .setItems(options, (dialog, which) -> {
                    promptForCustomMessage(which);
                })
                .show();
    }

    private void promptForCustomMessage(int optionIndex) {
        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Enter message (optional)");
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Custom message")
                .setView(input)
                .setPositiveButton("Send", (d, w) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        message = "Update for " + (eventForOrg.getName() != null ? eventForOrg.getName() : "this event");
                    }
                    sendNotification(optionIndex, message);
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void sendNotification(int optionIndex, String message) {
        switch (optionIndex) {
            case 0:
                eventService.notifyWaitlistEntrants(eventForOrg.getEventId(), message)
                        .addOnSuccessListener(ignored -> Toast.makeText(getContext(),
                                "Notified waitlist entrants.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                "Failed to notify waitlist.", Toast.LENGTH_SHORT).show());
                break;
            case 1:
                eventService.notifySelectedEntrants(eventForOrg.getEventId(), message)
                        .addOnSuccessListener(ignored -> Toast.makeText(getContext(),
                                "Notified selected entrants.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                "Failed to notify selected entrants.", Toast.LENGTH_SHORT).show());
                break;
            case 2:
                eventService.notifyCancelledEntrants(eventForOrg.getEventId(), message)
                        .addOnSuccessListener(ignored -> Toast.makeText(getContext(),
                                "Notified cancelled entrants.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                "Failed to notify cancelled entrants.", Toast.LENGTH_SHORT).show());
                break;
            default:
                break;
        }
    }
    private void exportEntrantsToCsv() {
        ArrayList<String> entrants = eventForOrg.getAccepted_Entrants();

        if (entrants == null || entrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique filename based on event name and date
        String fileName = "entrants_" + eventForOrg.getName().replaceAll("\\s+", "_") + ".csv";

        // Use StringBuilder to efficiently build the CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Accepted Entrants\n"); // CSV Header
        for (String entrantId : entrants) {
            csvContent.append(entrantId).append("\n");
        }

        // Use MediaStore to save the file to the public "Downloads" directory
        // This is the modern, recommended way to handle file saving.
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");

        Uri uri = getContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        if (uri != null) {
            try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(csvContent.toString().getBytes());
                    Toast.makeText(getContext(), "Exported to Downloads folder!", Toast.LENGTH_LONG).show();
                    dismiss(); // Close dialog on success
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
