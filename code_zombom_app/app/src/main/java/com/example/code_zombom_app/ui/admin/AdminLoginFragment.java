package com.example.code_zombom_app.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginFragment extends Fragment {

    private EditText passwordInput;
    private Button loginButton;
    private TextView errorText;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        passwordInput = view.findViewById(R.id.admin_password_input);
        loginButton = view.findViewById(R.id.button_admin_login);
        errorText = view.findViewById(R.id.text_error_message);

        loginButton.setOnClickListener(v -> verifyAdminCode());
    }

    private void verifyAdminCode() {
        String inputCode = passwordInput.getText().toString().trim();

        if (inputCode.isEmpty()) {
            passwordInput.setError("Code required");
            return;
        }

        // Hide error while checking
        errorText.setVisibility(View.GONE);
        loginButton.setEnabled(false); // Prevent double clicks

        // Check Firestore: Collection "AdminData", Document "security"
        db.collection("AdminData").document("security")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String realCode = documentSnapshot.getString("adminCode");

                        if (realCode != null && realCode.equals(inputCode)) {
                            // SUCCESS: Navigate to the Dashboard
                            navigateToAdminDashboard();
                        } else {
                            // FAIL: Wrong code
                            showError("Invalid Admin Code");
                        }
                    } else {
                        // Document doesn't exist (Forgot Step 1?)
                        showError("System Error: No Admin Config found.");
                    }
                    loginButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    showError("Connection Failed");
                    Log.e("AdminLogin", "Error", e);
                    loginButton.setEnabled(true);
                });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void navigateToAdminDashboard() {
        Toast.makeText(getContext(), "Admin Access Granted", Toast.LENGTH_SHORT).show();

        // Navigate to your existing Tab View or Admin Home Fragment
        // Assuming you have a fragment that holds the Profile/Event tabs:
        Fragment adminHomeFragment = new AdminHomeFragment(); // OR whatever your main Admin container is called

        // If you don't have a container and just want to go straight to Profiles:
        // Fragment adminHomeFragment = new ProfileAdminFragment();

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment_activity_main, adminHomeFragment); // Ensure this ID matches your Activity's container ID
        transaction.addToBackStack(null);
        transaction.commit();
    }
}