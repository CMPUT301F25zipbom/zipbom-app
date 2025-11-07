package com.example.code_zombom_app.organizer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.R;

public class OrganizerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_organizer_main);

        if(savedInstanceState == null){
            OrganizerMainFragment fragment = new OrganizerMainFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.organizerMainFragment, fragment);
            transaction.commit();
        }
    }
}
