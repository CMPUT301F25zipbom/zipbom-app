package com.example.code_zombom_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.stream.Collectors;

import com.example.code_zombom_app.Helpers.Event.Event;

/**
 * Simple adapter that renders a catalog of events for entrants to browse and join.
 */
public class EntrantEventAdapter extends ListAdapter<Event, EntrantEventAdapter.EventViewHolder> {

    /**
     * Callback for item interactions from the event catalog list.
     */
    public interface OnEventActionListener {
        void onEventSelected(@NonNull Event event);
        void onJoinWaitingList(@NonNull Event event);
        void onLeaveWaitingList(@NonNull Event event);
    }

    private final OnEventActionListener actionListener;

    /**
     * @param actionListener receiver for item tap events and join clicks
     */
    public EntrantEventAdapter(OnEventActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entrant_listview_events, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        holder.bind(event, actionListener);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTextView;
        private final TextView categoriesTextView;
        private final TextView detailsTextView;
        private final Button joinButton;
        private final Button leaveButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textView_listView_event_name);
            categoriesTextView = itemView.findViewById(R.id.textView_listView_event_genre);
            detailsTextView = itemView.findViewById(R.id.textView_listView_event_details);
            joinButton = itemView.findViewById(R.id.button_listView_event_join_waitingList);
            leaveButton = itemView.findViewById(R.id.button_listView_event_leave_waitingList);
        }

        /**
         * Binds a single event row and wires the click handlers for the supplied listener.
         */
        void bind(@NonNull Event event, OnEventActionListener actionListener) {
            nameTextView.setText(event.getName());
            //categoriesTextView.setText(buildCategoryLabel(event.getCategories()));

            String details = String.format(
                    Locale.getDefault(),
                    "%d waiting • Max %d",
                    event.getNumberOfWaiting(),
                    event.getCapacity());
            detailsTextView.setText(details);

            itemView.setOnClickListener(v -> actionListener.onEventSelected(event));
            joinButton.setOnClickListener(v -> actionListener.onJoinWaitingList(event));
            leaveButton.setOnClickListener(v -> actionListener.onLeaveWaitingList(event));
        }

        /**
         * Builds a human-readable category label for the card, falling back to a default when none.
         */
        private String buildCategoryLabel(List<String> categories) {
            if (categories == null || categories.isEmpty()) {
                return itemView.getContext().getString(R.string.no_categories_assigned);
            }

            if (categories.size() == 1) {
                return categories.get(0);
            }

            return categories.stream().limit(3).collect(Collectors.joining(" • "));
        }
    }

    private static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Event>() {
                @Override
                public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                    if (oldItem.getEventId() == null) {
                        return oldItem == newItem;
                    }
                    return oldItem.getEventId().equals(newItem.getEventId());
                }

        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
//            if (oldItem == newItem) {
//                return true;
//            }

            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getLocation(), newItem.getLocation())
                    && oldItem.getCapacity() == newItem.getCapacity()
                    //&& Objects.equals(oldItem.getCategories(), newItem.getCategories())
                    && Objects.equals(oldItem.getRestrictions(), newItem.getRestrictions());
                    //&& Objects.equals(oldItem.getEventDateText(), newItem.getEventDateText())
                    //&& Objects.equals(oldItem.getRegistrationClosesAtText(), newItem.getRegistrationClosesAtText())
                    //&& oldItem.getNumberOfWaiting() == newItem.getNumberOfWaiting();
        }
    };
}
