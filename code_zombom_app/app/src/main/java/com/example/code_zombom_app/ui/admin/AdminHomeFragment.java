package com.example.code_zombom_app.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.R;

/**
 * The central dashboard for Administrator functionality.
 * This fragment acts as a container that allows the admin to toggle between
 * managing Events, Profiles, Posters, and viewing Notification Logs.
 */
public class AdminHomeFragment extends Fragment {

    public AdminHomeFragment() {
        super(R.layout.fragment_admin_home);
    }

    /**
     * Called immediately after the view has been created.
     * Initializes the navigation buttons and sets up the default view.
     *
     * @param view               The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button eventsBtn = view.findViewById(R.id.button_home_events);
        Button profilesBtn = view.findViewById(R.id.button_home_profiles);
        Button postersBtn = view.findViewById(R.id.button_home_posters);
        Button logsBtn = view.findViewById(R.id.button_home_logs);

        eventsBtn.setOnClickListener(v -> loadPanel(new EventsAdminFragment()));
        profilesBtn.setOnClickListener(v -> loadPanel(new ProfileAdminFragment()));
        postersBtn.setOnClickListener(v -> loadPanel(new PostersAdminFragment()));
        logsBtn.setOnClickListener(v -> loadPanel(new AdminNotificationLogsFragment()));

        // Load the Events panel by default on first creation
        if (savedInstanceState == null) {
            loadPanel(new EventsAdminFragment());
        }
    }

    /**
     * Replaces the current child fragment within the admin panel container.
     * Uses getChildFragmentManager() to ensure the fragments are nested correctly
     * within the AdminHomeFragment layout.
     *
     * @param fragment The new Fragment to display (Events, Profiles, Posters, or Logs).
     */
    private void loadPanel(Fragment fragment) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.admin_panel_container, fragment);
        ft.commit();
    }
}