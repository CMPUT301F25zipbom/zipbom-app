package com.example.code_zombom_app.Entrant.EditProfile;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.code_zombom_app.Helpers.MVC.GController;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;

public class EditProfileController extends GController<LoadUploadProfileModel> {
    private ImageButton imageButtonEditName;
    private ImageButton imageButtonEditEmail;
    private ImageButton imageButtonEditPhone;
    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewPhone;
    private Button buttonBack;
    private Button buttonSave;
    private Button buttonDeleteProfile;
    private ToggleButton toggleButtonNotification;
    private ToggleButton toggleButtonLinkDevice;
    private final String email;
    private Entrant entrant;

    public EditProfileController(LoadUploadProfileModel model,
                                 ImageButton editName, ImageButton editEmail, ImageButton editPhone,
                                 TextView Name, TextView Email, TextView Phone,
                                 Button back, Button save, Button delete,
                                 ToggleButton notification, ToggleButton link,
                                 String email) {
        super(model);

        this.imageButtonEditName = editName;
        this.imageButtonEditEmail = editEmail;
        this.imageButtonEditPhone = editPhone;
        this.textViewName = Name;
        this.textViewEmail = Email;
        this.textViewPhone = Phone;
        this.buttonBack = back;
        this.buttonSave = save;
        this.buttonDeleteProfile = delete;
        this.toggleButtonNotification = notification;
        this.toggleButtonLinkDevice = link;
        this.email = email;

        /* Load the current profile */
        ((LoadUploadProfileModel) model).loadProfile(email);
        entrant = (Entrant) ((LoadUploadProfileModel) model).getInterMsg("Profile");

        this.buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LoadUploadProfileModel) model).close();
            }
        });

        this.imageButtonEditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LoadUploadProfileModel) model).
            }
        });
    }

    /**
     * Create a popup window that prompts the users to edit a field
     *
     * @param title   The title of the popup window
     * @param message The message to ask the users
     */
}
