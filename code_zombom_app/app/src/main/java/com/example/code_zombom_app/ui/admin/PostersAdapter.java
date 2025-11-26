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

public class PostersAdapter extends RecyclerView.Adapter<PostersAdapter.PosterViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> eventList;
    private final OnPosterDeleteListener deleteListener;

    public interface OnPosterDeleteListener {
        void onDeleteClick(DocumentSnapshot eventSnapshot);
    }

    public PostersAdapter(Context context, List<DocumentSnapshot> eventList, OnPosterDeleteListener deleteListener) {
        this.context = context;
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public PosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_poster, parent, false);
        return new PosterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PosterViewHolder holder, int position) {
        DocumentSnapshot snapshot = eventList.get(position);

        String posterUrl = snapshot.getString("posterUrl");
        String eventName = snapshot.getString("name");

        holder.eventName.setText(eventName != null ? eventName : "Unknown Event");

        // Load image using Glide
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(context)
                    .load(posterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background) // Add a placeholder drawable if you have one
                    .into(holder.posterImage);
        }

        // Handle Delete Click
        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDeleteClick(snapshot));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

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