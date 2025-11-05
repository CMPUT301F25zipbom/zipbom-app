package com.example.code_zombom_app.LogIn;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.code_zombom_app.MVC.TController;

/**
 * The controller for the login process
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see TController
 */
public class LoginController extends TController<LoginModel> {
    private final EditText editTextemail;
    private final Button buttonLogin;
    private final Button buttonSignUp;
    private final Button buttonSignUpWithDevice;

    public LoginController(LoginModel M,
                           EditText email, Button login, Button signup, Button signUpDevice) {
        super(M);
        editTextemail = email;
        buttonLogin = login;
        buttonSignUp = signup;
        buttonSignUpWithDevice = signUpDevice;

        editTextemail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    /* Collapse the keyboard after done */
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.loadProfile(getEmailInput());
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /*...*/
            }
        });
    }

    /**
     * Get the input email address from the users
     *
     * @return The users's input
     * @see EditText
     */
    private String getEmailInput() {
        return editTextemail.getText().toString().trim();
    }

}
