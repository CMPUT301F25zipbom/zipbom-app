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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        posterEvents = new ArrayList<>();

        recyclerView = view.findViewById(R.id.recycler_view_posters);
        progressBar = view.findViewById(R.id.progress_bar_posters);

        // 2 Columns for the grid
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new PostersAdapter(getContext(), posterEvents, this::confirmDeletePoster);
        recyclerView.setAdapter(adapter);

        fetchPosters();
    }

    private void fetchPosters() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("Events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posterEvents.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // We check the raw field string, similar to how the coworker checked getPosterUrl()
                        String url = doc.getString("posterUrl");

                        // Only add to the list if there is a valid URL
                        if (url != null && !url.isEmpty()) {
                            posterEvents.add(doc);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);

                    if (posterEvents.isEmpty()) {
                        Toast.makeText(getContext(), "No events with posters found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching posters", e);
                    Toast.makeText(getContext(), "Error loading posters.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void confirmDeletePoster(DocumentSnapshot eventSnapshot) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Poster")
                .setMessage("Are you sure you want to remove this poster image?")
                .setPositiveButton("Delete", (dialog, which) -> deletePosterImage(eventSnapshot))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePosterImage(DocumentSnapshot eventSnapshot) {
        String posterUrl = eventSnapshot.getString("posterUrl");
        String eventId = eventSnapshot.getId();

        if (posterUrl == null || posterUrl.isEmpty()) return;

        // 1. Delete from Storage
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(posterUrl);
        imageRef.delete().addOnSuccessListener(aVoid -> {
            // 2. Delete reference from Database
            removePosterReferenceFromFirestore(eventId);
        }).addOnFailureListener(e -> {
            // If it fails to delete file, remove from DB anyway
            Toast.makeText(getContext(), "Storage delete failed, removing from DB...", Toast.LENGTH_SHORT).show();
            removePosterReferenceFromFirestore(eventId);
        });
    }

    private void removePosterReferenceFromFirestore(String eventId) {

        db.collection("Events").document(eventId)
                .update("posterUrl", null) // Set field to null
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Poster removed successfully", Toast.LENGTH_SHORT).show();
                    fetchPosters(); // Refresh list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update database", Toast.LENGTH_SHORT).show();
                });
    }
}