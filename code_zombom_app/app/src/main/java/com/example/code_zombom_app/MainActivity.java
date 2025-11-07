package com.example.code_zombom_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Hosts the primary navigation graph so the home screen can act as the hub
 * to the rest of the experience.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
