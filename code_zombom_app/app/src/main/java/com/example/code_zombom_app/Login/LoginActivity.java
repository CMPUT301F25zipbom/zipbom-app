package com.example.code_zombom_app.Login;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.MVC.TView;
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
public class LoginActivity extends AppCompatActivity implements TView<LoginModel> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login); // Inflate the UI view

        LoginModel model = new LoginModel(FirebaseFirestore.getInstance());
        LoginController controller = new LoginController(model,
                findViewById(R.id.editTextEmailAddress),
                findViewById(R.id.buttonLogIn),
                findViewById(R.id.buttonSignUp),
                findViewById(R.id.buttonSignInWithDevice),
                this);

        model.addView(this);
    }

    @Override
    public void update(LoginModel model) {
        if (model.getState() == LoginModel.State.LOGIN_SUCCESS) {
            finish(); // Return to the main activity
        } else if (model.getState() == LoginModel.State.LOGIN_FAILURE) {
            Toast.makeText(this, "Login failed: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
