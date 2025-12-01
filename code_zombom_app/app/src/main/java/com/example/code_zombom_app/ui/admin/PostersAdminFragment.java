package com.example.code_zombom_app.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_zombom_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing event posters.
 * Displays a grid of all events that have an uploaded poster, allowing admins
 * to view and delete inappropriate images.
 */
public class PostersAdminFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private PostersAdapter adapter;
    private List<DocumentSnapshot> posterEvents;
    private FirebaseFirestore db;
    private static final String TAG = "PostersAdminFragment";

    public PostersAdminFragment() {
        super(R.layout.fragment_posters_admin);
    }

    /**
     * Initializes the RecyclerView grid layout and fetches poster data.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        posterEvents = new ArrayList<>();

        recyclerView = view.findViewById(R.id.recycler_view_posters);
        progressBar = view.findViewById(R.id.progress_bar_posters);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new PostersAdapter(getContext(), posterEvents, this::confirmDeletePoster);
        recyclerView.setAdapter(adapter);

        fetchPosters();
    }

    /**
     * Fetches events from Firestore that contain a 'posterUrl'.
     * Updates the adapter upon successful retrieval.
     */
    private void fetchPosters() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("Events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) {
                        return;
                    }

                    posterEvents.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("posterUrl");
                        if (url != null && !url.isEmpty()) {
                            posterEvents.add(doc);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (posterEvents.isEmpty()) {
                        Toast.makeText(getContext(), "No events with posters found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.e(TAG, "Error fetching posters", e);
                    Toast.makeText(getContext(), "Error loading posters.", Toast.LENGTH_SHORT).show();
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Shows a confirmation dialog before deleting a poster image.
     */
    private void confirmDeletePoster(DocumentSnapshot eventSnapshot) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Poster")
                .setMessage("Are you sure you want to remove this poster image?")
                .setPositiveButton("Delete", (dialog, which) -> deletePosterImage(eventSnapshot))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the poster image from Firebase Storage first, then updates the Firestore document
     * to remove the reference.
     */
    private void deletePosterImage(DocumentSnapshot eventSnapshot) {
        String posterUrl = eventSnapshot.getString("posterUrl");
        String eventId = eventSnapshot.getId();

        if (posterUrl == null || posterUrl.isEmpty()) return;

        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(posterUrl);
        imageRef.delete().addOnSuccessListener(aVoid -> {
            removePosterReferenceFromFirestore(eventId);
        }).addOnFailureListener(e -> {
            if (!isAdded() || getContext() == null) return;

            Toast.makeText(getContext(), "Storage delete failed, removing from DB...", Toast.LENGTH_SHORT).show();
            removePosterReferenceFromFirestore(eventId);
        });
    }

    /**
     * Updates the Event document in Firestore to set 'posterUrl' to null.
     */
    private void removePosterReferenceFromFirestore(String eventId) {
        db.collection("Events").document(eventId)
                .update("posterUrl", null)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;

                    Toast.makeText(getContext(), "Poster removed successfully", Toast.LENGTH_SHORT).show();
                    fetchPosters();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;

                    Toast.makeText(getContext(), "Failed to update database", Toast.LENGTH_SHORT).show();
                });
    }
}