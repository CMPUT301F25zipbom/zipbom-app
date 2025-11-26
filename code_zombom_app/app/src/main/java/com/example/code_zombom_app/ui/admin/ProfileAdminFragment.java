package com.example.code_zombom_app.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

/**
 * Admin UI for managing PROFILES.
 * Displays all profiles stored in Firestore.
 * Allows admin to view details and delete profiles with reason input.
 */
public class ProfileAdminFragment extends Fragment {

    private LinearLayout profilesContainer;
    private FirebaseFirestore db;
    private CollectionReference profilesDb;
    private static final String TAG = "ProfileAdminFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Setup programmatic layout
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#4CAF50")); // Green background
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        profilesContainer = new LinearLayout(requireContext());
        profilesContainer.setOrientation(LinearLayout.VERTICAL);
        profilesContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollView.addView(profilesContainer);
        rootLayout.addView(scrollView);

        return rootLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Ensure this matches your collection name exactly.
        // Based on your previous output, it seems to be "Profiles"
        profilesDb = db.collection("Profiles");

        loadProfilesFromDatabase();
    }

    private void loadProfilesFromDatabase() {
        profilesDb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error loading profiles", error);
                return;
            }

            if (profilesContainer != null) {
                profilesContainer.removeAllViews();
            }

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    // match to database
                    String name = snapshot.getString("name");
                    String email = snapshot.getString("email");
                    String type = snapshot.getString("type"); // DB uses "type", not "Role"

                    // Fallback if data is missing
                    if (name == null) name = "Unknown Name";
                    if (email == null) email = "No Email";
                    if (type == null) type = "Unknown Type";

                    StringBuilder profileText = new StringBuilder();
                    profileText.append("Name: ").append(name).append("\n")
                            .append("Email: ").append(email).append("\n")
                            .append("Role: ").append(type);

                    // Inflate the row item
                    View profileView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.profile_admin_list, profilesContainer, false);

                    TextView textView = profileView.findViewById(R.id.profile_item_textview);
                    ImageButton deleteButton = profileView.findViewById(R.id.button_delete_profile);

                    textView.setText(profileText.toString());
                    textView.setOnClickListener(v -> showProfileDetailsDialog(snapshot));
                    deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(snapshot));

                    profilesContainer.addView(profileView);
                }
            } else {
                // Show "No profiles" message
                TextView noProfiles = new TextView(requireContext());
                noProfiles.setText("No profiles found.");
                noProfiles.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                noProfiles.setTextSize(18);
                noProfiles.setPadding(32, 32, 32, 32);
                profilesContainer.addView(noProfiles);
            }
        });
    }

    private void showDeleteConfirmationDialog(QueryDocumentSnapshot snapshot) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete this profile?")
                .setPositiveButton("Yes", (dialog, which) -> showReasonInputDialog(snapshot))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showReasonInputDialog(QueryDocumentSnapshot snapshot) {
        final EditText input = new EditText(requireContext());
        input.setHint("Enter reason for deleting this profile");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Reason for deletion")
                .setMessage("Please specify why you are deleting this profile:")
                .setView(input)
                .setPositiveButton("Confirm delete", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(requireContext(), "Deletion cancelled — reason required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d(TAG, "Deleting profile " + snapshot.getId() + " because: " + reason);

                    profilesDb.document(snapshot.getId()).delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showProfileDetailsDialog(QueryDocumentSnapshot snapshot) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.profile_pop_up, null);

        TextView title = dialogView.findViewById(R.id.profile_details_title); // Ensure this ID exists in XML
        TextView body = dialogView.findViewById(R.id.profile_details_body);
        View notifBtn = dialogView.findViewById(R.id.button_notification_logs);
        View cancelBtn = dialogView.findViewById(R.id.button_cancel_profile_dialog);

        // parse correcly
        String name = snapshot.getString("name");
        String email = snapshot.getString("email");
        String phone = snapshot.getString("phone");

        // Handle Arrays safely
        List<String> eventHistory = (List<String>) snapshot.get("eventHistory");
        String eventHistoryStr = (eventHistory != null && !eventHistory.isEmpty())
                ? TextUtils.join(", ", eventHistory)
                : "None";

        String details = "Name: " + name + "\n" +
                "Email: " + email + "\n" +
                "Phone: " + (phone != null ? phone : "N/A") + "\n\n" +
                "Event History IDs:\n" + eventHistoryStr;

        body.setText(details);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        notifBtn.setOnClickListener(v -> {
            // TODO: Navigate to notification logs
            Toast.makeText(getContext(), "Notification logs coming soon", Toast.LENGTH_SHORT).show();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}