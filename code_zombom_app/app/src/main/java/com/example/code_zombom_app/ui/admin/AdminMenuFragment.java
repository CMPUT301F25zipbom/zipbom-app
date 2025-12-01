package com.example.code_zombom_app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.R;

/**
 * A simplified navigation menu for the Admin interface.
 * Allows switching between Events and Profiles management views.
 */
public class AdminMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_menu, container, false);
    }

    /**
     * Sets up the navigation buttons for Events and Profiles.
     * Loads the Events fragment by default.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button eventsBtn = view.findViewById(R.id.button_events);
        Button profilesBtn = view.findViewById(R.id.button_profiles);

        // Default view: load Events panel
        loadInnerFragment(new EventsAdminFragment());

        eventsBtn.setOnClickListener(v -> loadInnerFragment(new EventsAdminFragment()));
        profilesBtn.setOnClickListener(v -> loadInnerFragment(new ProfileAdminFragment()));
    }

    /**
     * Helper method to replace the content of the admin container.
     *
     * @param fragment The fragment to display (EventsAdminFragment or ProfileAdminFragment).
     */
    private void loadInnerFragment(Fragment fragment) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.admin_content_container, fragment);
        ft.commit();
    }
}