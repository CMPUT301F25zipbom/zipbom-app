package com.example.code_zombom_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Hosts the primary navigation graph so the home screen can act as the hub
 * to the rest of the experience.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
