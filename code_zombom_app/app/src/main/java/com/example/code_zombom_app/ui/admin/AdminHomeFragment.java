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

        Button eventsBtn = view.findViewById(R.id.button_home_events);
        Button profilesBtn = view.findViewById(R.id.button_home_profiles);
        Button postersBtn = view.findViewById(R.id.button_home_posters);

        // Default view
        eventsBtn.setOnClickListener(v -> loadPanel(new EventsAdminFragment()));
        profilesBtn.setOnClickListener(v -> loadPanel(new ProfileAdminFragment()));
        postersBtn.setOnClickListener(v -> loadPanel(new PostersAdminFragment()));
    }

    private void loadPanel(Fragment fragment) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.admin_panel_container, fragment);
        ft.commit();
    }
}
