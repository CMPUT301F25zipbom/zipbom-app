package com.example.code_zombom_app.Entrant;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Entrant.EditProfile.EditProfileActivity;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class EntrantMainActivity extends AppCompatActivity implements TView<EntrantMainModel> {
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_main);

        email = getIntent().getStringExtra("Email"); // Get the email address

        EntrantMainModel model = new EntrantMainModel(FirebaseFirestore.getInstance());
        EntrantMainController controller = new EntrantMainController(model,
                findViewById(R.id.imageButtonFilter),
                findViewById(R.id.imageButtonProfile),
                findViewById(R.id.imageButtonCamera));
        model.addView(this);
    }

    @Override
    public void update(EntrantMainModel model) {
        if (model.getState() == GModel.State.OPEN)
            if (model.getInterMsg("Extra") != null)
                if (model.getInterMsg("Extra").equals("Profile")) {
                    Intent editProfile = new Intent(this, EditProfileActivity.class);
                    editProfile.putExtra("Email", email);
                    startActivity(editProfile);
                }
    }
}
