package com.example.code_zombom_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

/**
 * Simple detail screen that mirrors the currently selected event from the shared ViewModel.
 */
public class EntrantEventDetailsFragment extends Fragment {

    private EntrantEventListViewModel viewModel;
    private TextView titleView;
    private TextView categoriesView;
    private TextView locationView;
    private TextView waitlistView;
    private TextView capacityView;
    private TextView dateView;
    private TextView deadlineView;
    private TextView descriptionView;
    private View notificationIcon;
    private ImageView posterView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleView = view.findViewById(R.id.detail_event_title);
        categoriesView = view.findViewById(R.id.detail_event_categories);
        locationView = view.findViewById(R.id.detail_event_location);
        waitlistView = view.findViewById(R.id.detail_event_waiting);
        capacityView = view.findViewById(R.id.detail_event_capacity);
        dateView = view.findViewById(R.id.detail_event_date);
        deadlineView = view.findViewById(R.id.detail_event_deadline);
        descriptionView = view.findViewById(R.id.detail_event_description);
        notificationIcon = view.findViewById(R.id.notification_icon);
        posterView = view.findViewById(R.id.detail_event_poster);

        viewModel = new ViewModelProvider(requireActivity()).get(EntrantEventListViewModel.class);
        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events == null) {
                return;
            }
            String eventId = getArguments() != null ? getArguments().getString("eventId") : null;
            if (eventId == null) {
                return;
            }
            for (Event event : events) {
                if (eventId.equals(event.getFirestoreDocumentId())) {
                    bindEvent(event);
                    break;
                }
            }
        });

        notificationIcon.setOnClickListener(v -> {
            viewModel.onNotificationDisplayed();
            String eventId = getArguments() != null ? getArguments().getString("eventId") : null;
            EntrantNotificationFragment fragment = EntrantNotificationFragment.newInstance(eventId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.entrant_event_list_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void bindEvent(@NonNull Event event) {
        titleView.setText(event.getName());
        categoriesView.setText(event.getCategories().isEmpty()
                ? getString(R.string.no_categories_assigned)
                : event.getCategories().toString());
        locationView.setText(getString(R.string.detail_location_format,
                event.getLocation() == null ? "" : event.getLocation()));
        String waitlist = getString(R.string.detail_waiting_format, event.getNumberOfWaiting());
        waitlistView.setText(waitlist);
        String capacity = getString(R.string.detail_capacity_format, event.getCapacity());
        capacityView.setText(capacity);
        dateView.setText(getString(R.string.detail_date_format, event.getEventDateText()));
        deadlineView.setText(getString(R.string.detail_deadline_format, event.getRegistrationClosesAtText()));
        String description = event.getDescription();
        if (description == null || description.isEmpty()) {
            description = getString(R.string.detail_description_placeholder);
        }
        descriptionView.setText(description);

        // Load poster when a URL is present, otherwise hide the container to avoid empty space.
        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            posterView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(posterUrl.trim())
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .centerCrop()
                    .into(posterView);
        } else {
            posterView.setVisibility(View.GONE);
        }
    }
}
