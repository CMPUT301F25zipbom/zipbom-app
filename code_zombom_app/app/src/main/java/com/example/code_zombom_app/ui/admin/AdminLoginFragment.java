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
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles Administrator authentication.
 * Verifies a security code entered by the user against the code stored in Firestore
 * before granting access to the Admin Dashboard.
 */
public class AdminLoginFragment extends Fragment {

    private EditText passwordInput;
    private Button loginButton;
    private TextView errorText;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_login_fragment, container, false);
    }

    /**
     * Initializes UI components and sets up the login button listener.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        passwordInput = view.findViewById(R.id.admin_password_input);
        loginButton = view.findViewById(R.id.button_admin_login);
        errorText = view.findViewById(R.id.text_error_message);

        loginButton.setOnClickListener(v -> verifyAdminCode());
    }

    /**
     * Validates the input code against the 'AdminData/security' document in Firestore.
     * If the code matches, navigates to the admin dashboard. Otherwise, displays an error.
     */
    private void verifyAdminCode() {
        String inputCode = passwordInput.getText().toString().trim();

        if (inputCode.isEmpty()) {
            passwordInput.setError("Code required");
            return;
        }

        errorText.setVisibility(View.GONE);
        loginButton.setEnabled(false);

        db.collection("AdminData").document("security")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String realCode = documentSnapshot.getString("adminCode");

                        if (realCode != null && realCode.equals(inputCode)) {
                            navigateToAdminDashboard();
                        } else {
                            showError("Invalid Admin Code");
                        }
                    } else {
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

    /**
     * Displays an error message to the user.
     *
     * @param message The error string to display.
     */
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    /**
     * Navigates the user to the AdminHomeFragment upon successful login.
     * Replaces the current fragment in the main activity container.
     */
    private void navigateToAdminDashboard() {
        Toast.makeText(getContext(), "Admin Access Granted", Toast.LENGTH_SHORT).show();

        Fragment adminHomeFragment = new AdminHomeFragment();

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment_activity_main, adminHomeFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}