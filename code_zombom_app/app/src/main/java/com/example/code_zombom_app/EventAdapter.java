package com.example.code_zombom_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil; // <-- Import DiffUtil
import androidx.recyclerview.widget.ListAdapter; // <-- Import ListAdapter
import androidx.recyclerview.widget.RecyclerView;

// 1. Change "RecyclerView.Adapter" to "ListAdapter"
public class EventAdapter extends ListAdapter<String, EventAdapter.EventViewHolder> {

    // 2. The list is now managed by ListAdapter, so you can remove this:
    // private List<String> eventList = new ArrayList<>();

    // 3. The constructor now needs a DiffUtil.ItemCallback
    public EventAdapter() {
        super(DIFF_CALLBACK);
    }

    // 4. Create the mandatory DIFF_CALLBACK object.
    // This tells the ListAdapter how to know if two items are the same or have the same contents.
    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            // For a list of strings, the items themselves are the unique identifier.
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            // If the items are the same, their contents are also the same.
            return oldItem.equals(newItem);
        }
    };


    // --- ViewHolder class remains exactly the same ---
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        public TextView eventTextView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTextView = itemView.findViewById(R.id.event_item_textview);
        }
    }

    // --- onCreateViewHolder remains exactly the same ---
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_list_item, parent, false);
        return new EventViewHolder(view);
    }

    // --- onBindViewHolder is slightly different ---
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        // 5. Use getItem(position) to get the data item for the ListAdapter
        String currentEvent = getItem(position);
        holder.eventTextView.setText(currentEvent);

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Clicked: " + currentEvent.substring(0, Math.min(currentEvent.length(), 20)) + "...", Toast.LENGTH_SHORT).show();
        });
    }
}
