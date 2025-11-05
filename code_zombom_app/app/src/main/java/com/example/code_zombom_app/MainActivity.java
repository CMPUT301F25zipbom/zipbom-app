package com.example.code_zombom_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Login.LoginActivity;

/**
 * The main activity of the project. For now we should use this activity to open other activity to
 * avoid code conflict and to make everything easier to merge later on.
 *
 * @author zipbom-team
 * @version 1.0.0, 11/4/2025
 * @see AppCompatActivity
 * @see LoginActivity
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Open the login activity */
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }



}