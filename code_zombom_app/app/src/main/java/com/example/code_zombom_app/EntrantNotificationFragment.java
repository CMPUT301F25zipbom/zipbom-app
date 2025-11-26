package com.example.code_zombom_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class EntrantNotificationFragment extends Fragment {

    private static final String ARG_EVENT_ID = "arg_event_id";

    public static EntrantNotificationFragment newInstance(@Nullable String eventId) {
        EntrantNotificationFragment fragment = new EntrantNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    private EntrantEventListViewModel viewModel;
    private TextView messageView;
    private TextView titleView;
    private Button acceptButton;
    private Button declineButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleView = view.findViewById(R.id.notification_event_name);
        messageView = view.findViewById(R.id.notification_message);
        Button backButton = view.findViewById(R.id.notification_back_button);
        acceptButton = view.findViewById(R.id.notification_accept_button);
        declineButton = view.findViewById(R.id.notification_decline_button);
        viewModel = new ViewModelProvider(requireActivity()).get(EntrantEventListViewModel.class);
        String eventId = getArguments() != null ? getArguments().getString(ARG_EVENT_ID) : null;
        viewModel.loadLatestNotification(eventId, new EntrantEventListViewModel.NotificationCallback() {
            @Override
            public void onNotificationLoaded(@Nullable EntrantEventListViewModel.EntrantNotification notification) {
                bindNotification(notification);
            }
        });

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        viewModel.getJoinStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                viewModel.clearJoinStatusMessage();
            }
        });

        viewModel.isJoinInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            if (acceptButton != null) {
                acceptButton.setEnabled(inProgress == null || !inProgress);
            }
            if (declineButton != null) {
                declineButton.setEnabled(inProgress == null || !inProgress);
            }
        });
    }

    private void bindNotification(@Nullable EntrantEventListViewModel.EntrantNotification notification) {
        acceptButton.setVisibility(View.GONE);
        acceptButton.setOnClickListener(null);
        declineButton.setVisibility(View.GONE);
        declineButton.setOnClickListener(null);

        if (notification == null) {
            titleView.setText(R.string.notification_title);
            messageView.setText(R.string.notification_default_message);
        } else {
            titleView.setText(notification.eventName == null ? getString(R.string.notification_title) : notification.eventName);
            messageView.setText(notification.message);
            if ("win".equalsIgnoreCase(notification.type)) {
                acceptButton.setVisibility(View.VISIBLE);
                acceptButton.setOnClickListener(v -> viewModel.acceptInvitation(notification.eventId, notification.eventName));
                declineButton.setVisibility(View.VISIBLE);
                declineButton.setOnClickListener(v -> viewModel.declineInvitation(notification.eventId, notification.eventName));
            }
        }
    }
}
