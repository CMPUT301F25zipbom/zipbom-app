package com.example.code_zombom_app.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * {@code ProfileAdminFragment} provides the administrator interface
 * for viewing and managing user profiles stored in Firestore.
 *
 * <p>This fragment dynamically displays a scrollable list of all profiles.
 * Each profile entry includes its details and a delete button that allows
 * the admin to remove that profile after confirming and providing a reason.</p>
 *
 * <p>Admin users can also tap a profile item to open a pop-up dialog
 * containing detailed profile information and further actions
 * (e.g., view notification logs, cancel).</p>
 */

public class ProfileAdminFragment extends Fragment {

    /** container that holds all profile views */
    private LinearLayout profilesContainer;
    /** reference to the firestore database */
    private FirebaseFirestore db;
    /** reference to the firestore collection that stores profiles */
    private CollectionReference profilesDb;



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

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        profilesDb = db.collection("Profiles"); // your Firestore collection name
        loadProfilesFromDatabase();
    }


    private void loadProfilesFromDatabase() {
        profilesDb.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("FirestoreProfiles", error.toString());
                return;
            }

            profilesContainer.removeAllViews();

            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    StringBuilder profileText = new StringBuilder();
                    profileText.append("Name: ").append(snapshot.getString("Name")).append("\n")
                            .append("Email: ").append(snapshot.getString("Email")).append("\n")
                            .append("Role: ").append(snapshot.getString("Role"));

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
                TextView noProfiles = new TextView(requireContext());
                noProfiles.setText("No profiles yet.");
                noProfiles.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                noProfiles.setTextSize(18);
                noProfiles.setPadding(16, 16, 16, 16);
                profilesContainer.addView(noProfiles);
            }
        });
    }

    private void showDeleteConfirmationDialog(QueryDocumentSnapshot snapshot) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete this profile?")
                .setPositiveButton("Yes", (dialog, which) -> showReasonInputDialog(snapshot))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showReasonInputDialog(QueryDocumentSnapshot snapshot) {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter reason for deleting this profile");
        input.setPadding(48, 24, 48, 24);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Reason for deletion")
                .setMessage("Please specify why you are deleting this profile:")
                .setView(input)
                .setPositiveButton("Confirm delete", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Deletion cancelled — reason required.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d("AdminProfileDelete",
                            "Deleting profile " + snapshot.getId() + " because: " + reason);

                    profilesDb.document(snapshot.getId()).delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(requireContext(),
                                            "Profile deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Failed to delete profile", Toast.LENGTH_SHORT).show());
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

        // fill from Firestore
        String name = snapshot.getString("Name");
        String events = snapshot.getString("Events"); // adjust field name if different

        body.setText("Name: " + name + "\nEvent(s) ID: " + (events != null ? events : "N/A"));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        notifBtn.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notification logs not implemented", Toast.LENGTH_SHORT).show();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
