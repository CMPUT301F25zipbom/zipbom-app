package com.example.code_zombom_app.Login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Entrant.EntrantMainActivity;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.example.code_zombom_app.Login.SignUp.SignUpActivity;
import com.example.code_zombom_app.MainActivity;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This is the login activity. Will return to the main activity after a successful login
 * <p>
 *     This is the Application and View level
 * </p>
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see AppCompatActivity
 * @see MainActivity
 */
public class LoginActivity extends AppCompatActivity implements TView<LoadUploadProfileModel> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login); // Inflate the UI view

        LoadUploadProfileModel model = new LoadUploadProfileModel(FirebaseFirestore.getInstance());
        LoginController controller = new LoginController(model,
                findViewById(R.id.editTextEmailAddress),
                findViewById(R.id.buttonLogIn),
                findViewById(R.id.buttonSignUp),
                findViewById(R.id.buttonSignInWithDevice));

        model.addView(this);
    }

    @Override
    public void update(LoadUploadProfileModel model) {
        if (model.getState() == GModel.State.LOGIN_SUCCESS) {
            Profile profile = (Profile) model.getInterMsg("Profile");
            String email = profile.getEmail();
            Toast.makeText(this, "Welcome " + email, Toast.LENGTH_SHORT).show();

            if (profile.getType().equals("Entrant")) {
                Intent entrantMain = new Intent(this, EntrantMainActivity.class);
                entrantMain.putExtra("Email", email); // Send the email address to the next activity
                startActivity(entrantMain);
                finish();
            }

        } else if (model.getState() == GModel.State.LOGIN_FAILURE) {
            Toast.makeText(this, "Login failed: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        }
        else if (model.getState() == GModel.State.OPEN) {
            Intent signUp = new Intent(this, SignUpActivity.class);
            startActivity(signUp);
        }
        else if (model.getState() == GModel.State.REQUEST_LOGIN_WITH_DEVICE_ID) {
            model.loadProfileWithDeviceId(this);
        }
    }
}
