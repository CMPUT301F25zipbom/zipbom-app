package com.example.code_zombom_app.Helpers.Event;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.code_zombom_app.R;

import java.util.List;

/**
 * A class to support the representation of an Event on a list view
 *
 * @author Dang Nguyen
 * @version 11/24/2025
 * @see Event
 * @see android.widget.ListView
 * @see android.widget.ArrayAdapter
 */
public class EventListAdapter extends ArrayAdapter<Event> {

    private final LayoutInflater inflater;
    private final EventService eventService;
    private final String email;

    // Holds per-row views
    private static class ViewHolder {
        TextView name;
        TextView genre;
        TextView numWaitList;
        TextView startDate;
        TextView endDate;
        TextView location;
        TextView details;
        Button joinButton;
        Button leaveButton;
    }

    public EventListAdapter(@NonNull Context context,
                            @NonNull List<Event> events,
                            @Nullable String entrant) {
        super(context, 0, events);
        this.inflater = LayoutInflater.from(context);
        this.eventService = new EventService();
        this.email = entrant;
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.listview_events, parent, false);
            holder = new ViewHolder();

            holder.name = convertView.findViewById(R.id.textView_listView_event_name);
            holder.genre = convertView.findViewById(R.id.textView_listView_event_genre);
            holder.numWaitList = convertView.findViewById(R.id.textView_listView_event_numWaitList);
            holder.startDate = convertView.findViewById(R.id.textView_listView_event_startDate);
            holder.endDate = convertView.findViewById(R.id.textView_listView_event_endDate);
            holder.location = convertView.findViewById(R.id.textView_listView_event_location);
            holder.details = convertView.findViewById(R.id.textView_listView_event_details);
            holder.joinButton = convertView.findViewById(R.id.button_listView_event_join_waitingList);
            holder.leaveButton = convertView.findViewById(R.id.button_listView_event_leave_waitingList);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Event event = getItem(position);
        if (event == null) {
            // Nothing to bind
            return convertView;
        }

        holder.name.setText("Name: " + event.getName());
        holder.genre.setText(event.getGenre());
        holder.genre.setText("Waiting list: " + event.getNumberOfWaiting() + "/" +
                event.getWaitlistLimit());

        if (event.getEventStartDate() != null) {
            holder.startDate.setText("Start date: " + event.getEventStartDate().toString());
        } else {
            holder.startDate.setText("Start date: -");
        }

        if (event.getEventEndDate() != null) {
            holder.endDate.setText("End date: " + event.getEventEndDate().toString());
        } else {
            holder.endDate.setText("End date: -");
        }

        if (event.getLocation() != null) {
            holder.location.setText("Location: " + event.getLocation().toString());
        } else {
            holder.location.setText("Location: -");
        }

        holder.details.setText("Descriptions: " + event.getDescription());

        if (email == null) {
            holder.joinButton.setEnabled(false);
            holder.leaveButton.setEnabled(false);
        } else {
            boolean alreadyInWaitlist = event.isInWaitingList(email);
            holder.joinButton.setEnabled(!alreadyInWaitlist);
            holder.leaveButton.setEnabled(alreadyInWaitlist);
        }

        holder.joinButton.setOnClickListener(v -> {
            if (email == null) {
                Toast.makeText(getContext(),
                        "Profile not loaded yet – please wait a moment",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("ADAPTER_BTN",
                    "Join clicked at position " + position + " for event " + event.getEventId());

            try {
                event.joinWaitingList(email);
                eventService.addEntrantToWaitlist(event.getEventId(), email);
                Toast.makeText(getContext(), "Join waiting list successfully",
                        Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
            } catch (RuntimeException e) {
                Log.e("Join Event Error", "Wait list is full", e);
                Toast.makeText(getContext(), "The wait list is full", Toast.LENGTH_SHORT).show();
            }
        });

        holder.leaveButton.setOnClickListener(v -> {
            if (email == null) {
                Toast.makeText(getContext(),
                        "Profile not loaded yet – please wait a moment",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("ADAPTER_BTN",
                    "Leave clicked at position " + position + " for event " + event.getEventId());

            event.leaveWaitingList(email);
            eventService.removeEntrantFromWaitlist(event.getEventId(), email);
            Toast.makeText(getContext(), "Leave waiting list successfully",
                    Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return convertView;
    }
}


