package com.example.code_zombom_app.Entrant.EditProfile;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.code_zombom_app.Helpers.MVC.GController;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;

public class EditProfileController extends GController<LoadUploadProfileModel> {
    private ImageButton imageButtonEditName;
    private ImageButton imageButtonEditEmail;
    private ImageButton imageButtonEditPhone;
    private TextView textViewName;
    private TextView textViewEmail;
    private TextView phone;
    private Button

    public EditProfileController(LoadUploadProfileModel model) {
        super(model);
    }
}
