package com.example.code_zombom_app.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * RecyclerView Adapter for displaying event posters in a grid.
 * Handles loading images via Glide and managing delete interactions.
 */
public class PostersAdapter extends RecyclerView.Adapter<PostersAdapter.PosterViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> eventList;
    private final OnPosterDeleteListener deleteListener;

    /**
     * Interface definition for a callback to be invoked when a poster delete button is clicked.
     */
    public interface OnPosterDeleteListener {
        void onDeleteClick(DocumentSnapshot eventSnapshot);
    }

    /**
     * Constructs a new PostersAdapter.
     *
     * @param context        The context used for layout inflation and image loading.
     * @param eventList      List of DocumentSnapshots containing poster URLs.
     * @param deleteListener Listener for delete actions.
     */
    public PostersAdapter(Context context, List<DocumentSnapshot> eventList, OnPosterDeleteListener deleteListener) {
        this.context = context;
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    /**
     * Creates a new ViewHolder for a poster item.
     */
    @NonNull
    @Override
    public PosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_poster, parent, false);
        return new PosterViewHolder(view);
    }

    /**
     * Binds data to the ViewHolder. Loads the poster image using Glide.
     */
    @Override
    public void onBindViewHolder(@NonNull PosterViewHolder holder, int position) {
        DocumentSnapshot snapshot = eventList.get(position);

        String posterUrl = snapshot.getString("posterUrl");
        String eventName = snapshot.getString("name");

        holder.eventName.setText(eventName != null ? eventName : "Unknown Event");

        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(context)
                    .load(posterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.posterImage);
        }

        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDeleteClick(snapshot));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * View Holder class for poster grid items.
     */
    public static class PosterViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImage;
        TextView eventName;
        ImageButton deleteBtn;

        public PosterViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.image_view_poster);
            eventName = itemView.findViewById(R.id.text_event_name);
            deleteBtn = itemView.findViewById(R.id.button_delete_poster);
        }
    }
}