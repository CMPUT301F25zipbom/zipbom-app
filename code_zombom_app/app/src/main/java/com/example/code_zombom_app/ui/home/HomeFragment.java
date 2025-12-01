package com.example.code_zombom_app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.code_zombom_app.ui.admin.AdminLoginFragment;
import com.example.code_zombom_app.Login.LoginActivity;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.databinding.FragmentHomeBinding;

/**
 * Entry point for the app's landing experience. Presents role-based cards (entrant, organizer,
 * admin) and routes the user to the correct flow. The fragment relies on Material cards and chips
for design.
 */
public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;

    private FragmentHomeBinding binding;

    /**
     * Inflates the view binding for the home fragment and retains it for later click wiring.
     *
     * @return root view attached to the binding
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }
    /**
     * Wires click listeners on the Entrant/Organizer/Admin chips so each one launches the expected flow.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View entrantChip = view.findViewById(R.id.chip_home_entrant);
        View organizerChip = view.findViewById(R.id.chip_home_organizer);
        View adminChip = view.findViewById(R.id.chip_home_admin);

        organizerChip.setOnClickListener(v -> {
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.action_home_to_events_graph);
        });

        adminChip.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, new AdminLoginFragment())
                    .addToBackStack(null) // Allows the user to press 'Back' to return to Home
                    .commit();
        });

        entrantChip.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
