package com.example.code_zombom_app.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.R;

public class AdminHomeFragment extends Fragment {

    public AdminHomeFragment() {
        super(R.layout.fragment_admin_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the buttons in the layout
        Button eventsBtn = view.findViewById(R.id.button_home_events);
        Button profilesBtn = view.findViewById(R.id.button_home_profiles);
        Button postersBtn = view.findViewById(R.id.button_home_posters);
        Button logsBtn = view.findViewById(R.id.button_home_logs); // <--- ADDED THIS

        // 2. Set Listeners
        eventsBtn.setOnClickListener(v -> loadPanel(new EventsAdminFragment()));
        profilesBtn.setOnClickListener(v -> loadPanel(new ProfileAdminFragment()));
        postersBtn.setOnClickListener(v -> loadPanel(new PostersAdminFragment()));

        // <--- ADDED THIS LISTENER
        logsBtn.setOnClickListener(v -> loadPanel(new AdminNotificationLogsFragment()));

        // 3. Default Load
        if (savedInstanceState == null) {
            loadPanel(new EventsAdminFragment());
        }
    }

    private void loadPanel(Fragment fragment) {
        // Using getChildFragmentManager() ensures the fragments stay inside the AdminHome container
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.admin_panel_container, fragment);
        ft.commit();
    }
}