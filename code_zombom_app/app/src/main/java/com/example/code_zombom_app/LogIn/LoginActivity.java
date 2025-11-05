package com.example.code_zombom_app.Login;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.LogIn.LoginController;
import com.example.code_zombom_app.LogIn.LoginModel;
import com.example.code_zombom_app.LogIn.LoginView;
import com.example.code_zombom_app.MVC.TView;
import com.example.code_zombom_app.MainActivity;
import com.example.code_zombom_app.R;

/**
 * This is the login activity. Will return to the main activity after a successful login
 * <p>
 *     This is the Application level
 * </p>
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see AppCompatActivity
 * @see MainActivity
 */
public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login); // Inflate the UI view

        LoginModel model = new LoginModel();
        LoginController controller = new LoginController(model,
                findViewById(R.id.editTextEmailAddress),
                findViewById(R.id.buttonLogIn),
                findViewById(R.id.buttonSignUp),
                findViewById(R.id.buttonSignInWithDevice));
        LoginView view = new LoginView(this);

        model.addView(view);
    }


}
