package com.example.code_zombom_app.LogIn;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.MVC.TView;

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
