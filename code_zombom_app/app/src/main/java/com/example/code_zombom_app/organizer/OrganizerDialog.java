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

public class OrganizerDialog extends Dialog {

    private final String eventId;
    private final String eventText;
    private final NavController navController;
    private final View fragmentView;

    public OrganizerDialog(@NonNull Context context, String eventId, String eventText, NavController navController, View fragmentView) {
        super(context);
        this.eventId = eventId;
        this.eventText = eventText;
        this.navController = navController;
        this.fragmentView = fragmentView;
    }

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

        // Set click listeners for each button
        viewStartButton.setOnClickListener(v -> {
            // TODO: Implement Draw
            dismiss(); // Close the dialog
        });

        messageButton.setOnClickListener(v -> {
            // TODO: Implement Message Entrants
            dismiss();
        });

        editEventButton.setOnClickListener(v -> {
            dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            bundle.putString("eventText", eventText);

            // Navigate to the edit fragment
            navController.navigate(R.id.action_organizerMainFragment_to_editEventFragment, bundle);
        });

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
}
