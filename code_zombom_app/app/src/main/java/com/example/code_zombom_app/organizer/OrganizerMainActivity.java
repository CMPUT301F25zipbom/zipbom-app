package com.example.code_zombom_app.organizer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.R;

public class OrganizerMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_organizer_main);

        // Only load the fragment if this is the first creation
        if (savedInstanceState == null) {
            OrganizerMainFragment fragment = new OrganizerMainFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.events_container_linearlayout, fragment); // container ID from XML
            transaction.commit();
        }
    }
}
