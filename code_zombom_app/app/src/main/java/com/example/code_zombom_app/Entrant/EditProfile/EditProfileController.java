package com.example.code_zombom_app.Entrant.EditProfile;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.code_zombom_app.Helpers.MVC.GController;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;

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

    public EditProfileController(LoadUploadProfileModel model,
                                 ImageButton editName, ImageButton editEmail, ImageButton editPhone,
                                 TextView Name, TextView Email, TextView Phone,
                                 Button back, Button save, Button delete,
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
        this.toggleButtonNotification = notification;
        this.toggleButtonLinkDevice = link;

        this.buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LoadUploadProfileModel) model).close();
            }
        });

        this.imageButtonEditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
