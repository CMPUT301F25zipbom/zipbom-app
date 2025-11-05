package com.example.code_zombom_app.Login;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.example.code_zombom_app.MVC.TView;

/**
 * The view of the login process
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see TView
 */
public class LoginView implements TView<LoginModel> {
    private final Context context;

    public LoginView(Context context) {
        this.context = context;
    }

    @Override
    public void update(LoginModel model) {
        if (model.getState() == LoginModel.State.LOGIN_SUCCESS) {
            ((Activity) context).finish(); // Return to the main activity
        } else if (model.getState() == LoginModel.State.LOGIN_FAILURE) {
            Toast.makeText(context, "Login failed!", Toast.LENGTH_SHORT).show();
        }
    }
}
