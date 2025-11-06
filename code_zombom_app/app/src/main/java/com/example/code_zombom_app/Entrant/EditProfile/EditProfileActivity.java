package com.example.code_zombom_app.Entrant.EditProfile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.R;

public class EditProfileActivity extends AppCompatActivity implements TView<LoadUploadProfileModel> {
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_profile);
        email = getIntent().getStringExtra("Email");
    }

    @Override
    public void update(LoadUploadProfileModel model) {

    }
}
