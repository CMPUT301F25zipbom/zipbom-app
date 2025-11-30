package com.example.code_zombom_app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.code_zombom_app.ui.admin.AdminNotificationLog;
import com.example.code_zombom_app.R;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.ViewHolder> {

    private final List<AdminNotificationLog> logs;

    public AdminNotificationLogAdapter(List<AdminNotificationLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminNotificationLog log = logs.get(position);

        holder.eventName.setText(log.getEventName() != null ? log.getEventName() : "Unknown Event");
        holder.recipient.setText("To: " + (log.getRecipientEmail() != null ? log.getRecipientEmail() : "N/A"));
        holder.message.setText(log.getMessage());
        holder.type.setText(log.getType() != null ? log.getType().toUpperCase() : "-");

        // Updated Date Logic
        if (log.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.date.setText(sdf.format(log.getCreatedAt()));
        } else {
            holder.date.setText("No Date");
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

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