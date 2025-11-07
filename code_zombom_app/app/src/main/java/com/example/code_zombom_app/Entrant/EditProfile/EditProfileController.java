package com.example.code_zombom_app.Entrant.EditProfile;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.code_zombom_app.Helpers.MVC.GController;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;

public class EditProfileController extends GController<EditProfileModel> {

    private ImageButton imageButtonEditName;
    private ImageButton imageButtonEditEmail;
    private ImageButton imageButtonEditPhone;
    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewPhone;
    private Button buttonBack;
    private Button buttonSave;
    private Button buttonDeleteProfile;
    private Button buttonLogout;
    private ToggleButton toggleButtonNotification;
    private ToggleButton toggleButtonLinkDevice;

    public EditProfileController(EditProfileModel model,
                                 ImageButton editName, ImageButton editEmail, ImageButton editPhone,
                                 TextView Name, TextView Email, TextView Phone,
                                 Button back, Button save, Button delete, Button logout,
                                 ToggleButton notification, ToggleButton link) {
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
        this.buttonLogout = logout;
        this.toggleButtonNotification = notification;
        this.toggleButtonLinkDevice = link;

        this.buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).close();
            }
        });

        this.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).editEntrant();
            }
        });

        //TODO: Create a popup window to ask if they are sure to delete the account
        this.buttonDeleteProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).deleteEntrant();
            }
        });

        this.buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).logout();
            }
        });

        this.imageButtonEditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).requestPopUp("Change name",
                        "Enter new name", "Name");
            }
        });

        this.imageButtonEditEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).requestPopUp("Change email",
                        "Enter new email", "Email");
            }
        });

        this.imageButtonEditPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditProfileModel) model).requestPopUp("Change phone number",
                        "Enter new phone number", "Phone");
            }
        });

        this.toggleButtonNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ((EditProfileModel) model).setNotification(isChecked);
        });

        this.toggleButtonLinkDevice.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            ((EditProfileModel) model).toggleLinkDeviceId(isChecked);
        }));
    }
}
