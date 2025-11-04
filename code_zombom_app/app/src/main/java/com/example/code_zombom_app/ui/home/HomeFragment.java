package com.example.code_zombom_app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <-- Import Button
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // <-- Import Nullable
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment; // <-- Import NavHostFragment

import com.example.code_zombom_app.R; // <-- Make sure this import is correct
import com.example.code_zombom_app.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;

    private FragmentHomeBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // All Code in onViewCreated() is temporary
        Button organizerButton = view.findViewById(R.id.toOrganizerUIForNow);

        organizerButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.action_home_to_events_graph);
        });

        binding.toAdminUI.setOnClickListener(v -> {
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.action_home_to_admin);
        });


    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}