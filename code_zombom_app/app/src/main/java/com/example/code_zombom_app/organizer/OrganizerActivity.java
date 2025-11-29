package com.example.code_zombom_app.organizer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.R;
import com.google.android.libraries.places.api.Places;

/**
 * @author Robert Enstrom, Tejwinder Johal
 * @version 1.0
 * This is the main activity for the organizer. It contains the organizer main fragment.
 * It exists to link the organizer section to the entrant activity.
 */
public class OrganizerActivity extends AppCompatActivity {
    /**
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_organizer_main);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), Location.getGoogleApi());
        }

        if(savedInstanceState == null){
            OrganizerMainFragment fragment = new OrganizerMainFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.organizerMainFragment, fragment);
            transaction.commit();
        }
    }
}
