package com.example.code_zombom_app.Helpers.Event;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
    private TextView textViewName;
    private TextView textViewGenre;
    private TextView textViewStartDate;
    private TextView textViewEndDate;
    private TextView textViewLocation;
    private TextView textViewDetails;

    public EventListAdapter(@NonNull Context context, @NonNull List<Event> events) {
        super(context, 0, events);
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

        if (event != null) {
            textViewName.setText(event.getName());
            textViewGenre.setText(event.getGenre());
            if (event.getEventStartDate() != null)
                textViewStartDate.setText(event.getEventStartDate().toString());
            if (event.getEventEndDate() != null)
                textViewEndDate.setText(event.getEventEndDate().toString());
            if (event.getLocation() != null)
                textViewLocation.setText(event.getLocation().toString());
            textViewDetails.setText(event.getDescription());
        }

        return convertView;
    }
}
