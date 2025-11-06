package com.example.code_zombom_app.Entrant.EditProfile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfileActivity extends AppCompatActivity implements TView<LoadUploadProfileModel> {
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_profile);
        email = getIntent().getStringExtra("Email");

        LoadUploadProfileModel model = new LoadUploadProfileModel(FirebaseFirestore.getInstance());
        EditProfileController controller = new EditProfileController(model,
                findViewById(R.id.imageButtonEntrantProfileName),
                findViewById(R.id.imageButtonEntrantProfileEmail),
                findViewById(R.id.imageButtonEntrantProfilePhone),
                findViewById(R.id.textViewEntrantProfileName),
                findViewById(R.id.textViewEntrantProfileEmail),
                findViewById(R.id.textViewEntrantProfilePhone),
                findViewById(R.id.buttonEntrantProfileBack),
                findViewById(R.id.buttonEntrantProfileSave),
                findViewById(R.id.buttonEntrantProfileDelete),
                findViewById(R.id.toggleButtonEntrantProfileNotification),
                findViewById(R.id.toggleButtonEntrantProfileLinkUnlinkDevice),
                email);
        model.addView(this);
    }

    @Override
    public void update(LoadUploadProfileModel model) {
        if (model.getState() == GModel.State.CLOSE)
            finish(); // Close the activity
    }
}
