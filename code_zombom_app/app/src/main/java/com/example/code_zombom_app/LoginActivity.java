package com.example.code_zombom_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.MVC.TView;

/**
 * This is the login activity. Will return to the main activity after a successful login
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see AppCompatActivity
 * @see MainActivity
 */
public class LoginActivity extends AppCompatActivity implements TView<MainModel> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login); // Inflate the UI view
    }
}
