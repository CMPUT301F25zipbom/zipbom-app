package com.example.code_zombom_app.Helpers.Event;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

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
    private String email;
    private EventService eventService;

    private TextView textViewName;
    private TextView textViewGenre;
    private TextView textViewStartDate;
    private TextView textViewEndDate;
    private TextView textViewLocation;
    private TextView textViewDetails;
    private Button buttonJoinWaitingList;
    private Button buttonLeaveWaitingList;

    /**
     *
     * @param context The adapter's context
     * @param events The list of events
     * @param email The entrant's email address
     */
    public EventListAdapter(@NonNull Context context, @NonNull List<Event> events,
                            String email) {
        super(context, 0, events);
        this.email = email;
        this.eventService = new EventService();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_events,
                    parent, false);

        Event event = getItem(position);

        textViewName = convertView.findViewById(R.id.textView_listView_event_name);
        textViewGenre = convertView.findViewById(R.id.textView_listView_event_genre);
        textViewStartDate = convertView.findViewById(R.id.textView_listView_event_startDate);
        textViewEndDate = convertView.findViewById(R.id.textView_listView_event_endDate);
        textViewLocation = convertView.findViewById(R.id.textView_listView_event_location);
        textViewDetails = convertView.findViewById(R.id.textView_listView_event_details);
        buttonJoinWaitingList = convertView.findViewById(R.id.button_listView_event_join_waitingList);
        buttonLeaveWaitingList = convertView.findViewById(R.id.button_listView_event_leave_waitingList);

        if (event != null) {
            textViewName.setText("Name: " + event.getName());
            textViewGenre.setText(event.getGenre());
            if (event.getEventStartDate() != null)
                textViewStartDate.setText("Start date: " + event.getEventStartDate().toString());
            if (event.getEventEndDate() != null)
                textViewEndDate.setText("End date: " + event.getEventEndDate().toString());
            if (event.getLocation() != null)
                textViewLocation.setText("Location: " + event.getLocation().toString());
            textViewDetails.setText("Descriptions: " + event.getDescription());
        }

        assert event != null;
        setFocusable(event);

        buttonJoinWaitingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event.joinWaitingList(email);
                eventService.addEntrantToWaitlist(event.getEventId(), email); // Update the database
                setFocusable(event);
                notifyDataSetChanged();
            }
        });

        buttonLeaveWaitingList.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                event.leaveWaitingList(email);
                eventService.removeEntrantFromWaitlist(event.getEventId(), email); // Update the database
                setFocusable(event);
                notifyDataSetChanged();
            }
        });

        return convertView;
    }

    /**
     * Disable/Enable the joining/leaving waiting list buttons
     *
     * @param event The event in the list view in this context
     */
    private void setFocusable(Event event) {
        if (event.isInWaitingList(email)) {
            buttonJoinWaitingList.setEnabled(false);
            buttonLeaveWaitingList.setEnabled(true);
        } else {
            buttonJoinWaitingList.setEnabled(true);
            buttonLeaveWaitingList.setEnabled(false);
        }
    }
}
