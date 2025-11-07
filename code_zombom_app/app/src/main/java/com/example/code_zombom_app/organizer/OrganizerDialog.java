package com.example.code_zombom_app.organizer;

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

import com.example.code_zombom_app.R;

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

    /**
     * This method is used to make an organizerdialog object.
     * @param context sets the context
     * @param eventId sets the organizerdialog eventid
     * @param eventText sets the organizerdialog eventtext
     * @param navController sets the organizerdialog navController
     * @param fragmentView sets the organizerdialog fragmentView
     */
    public OrganizerDialog(@NonNull Context context, String eventId, String eventText, NavController navController, View fragmentView) {
        super(context);
        this.eventId = eventId;
        this.eventText = eventText;
        this.navController = navController;
        this.fragmentView = fragmentView;
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
            // We need the fragment's root view to find the tag
            ImageView qrToShow = fragmentView.findViewWithTag(eventId);
            if (qrToShow != null) {
                if (qrToShow.getVisibility() == View.GONE) {
                    qrToShow.setVisibility(View.VISIBLE);
                }
            }
        });
        //This gets rid of the popup.
        cancelButton.setOnClickListener(v -> dismiss());

        // Make the dialog's background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
