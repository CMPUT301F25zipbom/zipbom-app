package com.example.code_zombom_app.Entrant;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Entrant.EditProfile.EditProfileActivity;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.R;

public class EntrantMainActivity extends AppCompatActivity implements TView<EntrantMainModel> {
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_main);

        email = getIntent().getStringExtra("Email"); // Get the email address
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
