package com.example.code_zombom_app.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class ProfileAdminFragment extends Fragment {

    private LinearLayout profilesContainer;
    private FirebaseFirestore db;
    private CollectionReference profilesDb;
    private static final String TAG = "ProfileAdminFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
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
        profilesDb = db.collection("Profiles");
        loadProfilesFromDatabase();
    }

    private void loadProfilesFromDatabase() {
        profilesDb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error loading profiles", error);
                return;
            }

            // --- FIX STARTS HERE ---
            // 1. Add this safety check. If the fragment isn't on screen, don't update UI.
            if (!isAdded() || getContext() == null) {
                return;
            }
            // --- FIX ENDS HERE ---

            if (profilesContainer != null) {
                profilesContainer.removeAllViews();
            }

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String email = snapshot.getString("email");
                    String type = snapshot.getString("type");

                    if (name == null) name = "Unknown Name";
                    if (email == null) email = "No Email";
                    if (type == null) type = "Unknown Type";

                    StringBuilder profileText = new StringBuilder();
                    profileText.append("Name: ").append(name).append("\n")
                            .append("Email: ").append(email).append("\n")
                            .append("Role: ").append(type);

                    // 2. Change requireContext() to getContext() to be safer (guaranteed not null due to check above)
                    View profileView = LayoutInflater.from(getContext())
                            .inflate(R.layout.profile_admin_list, profilesContainer, false);

                    TextView textView = profileView.findViewById(R.id.profile_item_textview);
                    ImageButton deleteButton = profileView.findViewById(R.id.button_delete_profile);

                    textView.setText(profileText.toString());
                    textView.setOnClickListener(v -> showProfileDetailsDialog(snapshot));
                    deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(snapshot));

                    profilesContainer.addView(profileView);
                }
            } else {
                // 3. Change requireContext() to getContext()
                TextView noProfiles = new TextView(getContext());
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
                    profilesDb.document(snapshot.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showProfileDetailsDialog(QueryDocumentSnapshot snapshot) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.profile_pop_up, null);

        TextView title = dialogView.findViewById(R.id.profile_details_title);
        TextView body = dialogView.findViewById(R.id.profile_details_body);
        View notifBtn = dialogView.findViewById(R.id.button_notification_logs);
        View cancelBtn = dialogView.findViewById(R.id.button_cancel_profile_dialog);

        // Get the EMAIL specifically for the query
        String name = snapshot.getString("name");
        String email = snapshot.getString("email");
        String phone = snapshot.getString("phone");

        Object historyObj = snapshot.get("eventHistory");
        String eventHistoryStr = "None";
        if (historyObj instanceof List) {
            List<String> list = (List<String>) historyObj;
            if (!list.isEmpty()) {
                eventHistoryStr = TextUtils.join(", ", list);
            }
        }

        String details = "Name: " + (name != null ? name : "Unknown") + "\n" +
                "Email: " + (email != null ? email : "N/A") + "\n" +
                "Phone: " + (phone != null ? phone : "N/A") + "\n\n" +
                "Event History IDs:\n" + eventHistoryStr;

        if (title != null) title.setText("Profile Details");
        if (body != null) body.setText(details);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (notifBtn != null) {
            // Check if email exists before querying
            if (email != null && !email.isEmpty()) {
                // Pass the EMAIL to the notification logs function
                notifBtn.setOnClickListener(v -> showNotificationLogs(email));
            } else {
                notifBtn.setOnClickListener(v ->
                        Toast.makeText(getContext(), "No email associated with this profile.", Toast.LENGTH_SHORT).show()
                );
            }
        }

        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    /**
     * Shows notification logs by querying the global 'Notifications' collection
     * where 'reciever' matches the profile email.
     */
    private void showNotificationLogs(String profileEmail) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.admin_notification_dialog, null);

        LinearLayout listContainer = dialogView.findViewById(R.id.notification_list_container);
        Button closeButton = dialogView.findViewById(R.id.button_close_notifications);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Query the root 'Notifications' collection for the specific email
        db.collection("Notifications")
                .whereEqualTo("reciever", profileEmail) // Uses exact spelling from your DB
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("No notification logs found for: " + profileEmail);
                        emptyView.setTextColor(Color.WHITE);
                        emptyView.setPadding(10, 10, 10, 10);
                        listContainer.addView(emptyView);
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            View itemView = LayoutInflater.from(getContext())
                                    .inflate(R.layout.admin_notification_item, listContainer, false);

                            TextView tvTitle = itemView.findViewById(R.id.notif_item_title);
                            TextView tvMessage = itemView.findViewById(R.id.notif_item_message);
                            TextView tvDate = itemView.findViewById(R.id.notif_item_date);

                            // Extract fields based on your provided data structure
                            String type = doc.getString("notificationtype");
                            String timeStr = doc.getString("time"); // "Fri Nov 28 23:32:51 MST 2025"

                            // Check if there is a message, otherwise construct one
                            String message = doc.getString("message");
                            if (message == null) {
                                message = "Sent via " + (type != null ? type : "system");
                            }

                            // Set Data
                            tvTitle.setText(type != null ? "Type: " + type : "Notification");
                            tvMessage.setText(message);
                            tvDate.setText(timeStr != null ? timeStr : "Unknown Date");

                            listContainer.addView(itemView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching notifications", e);
                    Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}