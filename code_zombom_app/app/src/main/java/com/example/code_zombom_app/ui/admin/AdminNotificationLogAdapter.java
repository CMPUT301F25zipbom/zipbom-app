package com.example.code_zombom_app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_zombom_app.R;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying {@link AdminNotificationLog} items.
 * Binds notification data to the views defined in the layout XML.
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.ViewHolder> {

    private final List<AdminNotificationLog> logs;

    /**
     * Constructs the adapter with a list of logs.
     * @param logs The list of AdminNotificationLog objects to display.
     */
    public AdminNotificationLogAdapter(List<AdminNotificationLog> logs) {
        this.logs = logs;
    }

    /**
     * Called when the RecyclerView needs a new {@link ViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * Formats the date and populates text views.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminNotificationLog log = logs.get(position);

        holder.eventName.setText(log.getEventName() != null ? log.getEventName() : "Unknown Event");
        holder.recipient.setText("To: " + (log.getRecipientEmail() != null ? log.getRecipientEmail() : "N/A"));
        holder.message.setText(log.getMessage());
        holder.type.setText(log.getType() != null ? log.getType().toUpperCase() : "-");

        if (log.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.date.setText(sdf.format(log.getCreatedAt()));
        } else {
            holder.date.setText("No Date");
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of logs.
     */
    @Override
    public int getItemCount() {
        return logs.size();
    }

    /**
     * ViewHolder class that describes an item view and metadata about its place within the RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, recipient, message, type, date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.text_log_event);
            recipient = itemView.findViewById(R.id.text_log_recipient);
            message = itemView.findViewById(R.id.text_log_message);
            type = itemView.findViewById(R.id.text_log_type);
            date = itemView.findViewById(R.id.text_log_date);
        }
    }
}