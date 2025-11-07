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

public class AdminMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button eventsBtn = view.findViewById(R.id.button_events);
        Button profilesBtn = view.findViewById(R.id.button_profiles);

        // Default view: load Events panel
        loadInnerFragment(new AdminFragment());

        eventsBtn.setOnClickListener(v -> loadInnerFragment(new AdminFragment()));
        profilesBtn.setOnClickListener(v -> loadInnerFragment(new ProfileAdminFragment()));
    }

    private void loadInnerFragment(Fragment fragment) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.admin_content_container, fragment);
        ft.commit();
    }
}
