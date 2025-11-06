package com.example.code_zombom_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_zombom_app.Helpers.Event.Event;

/**
 * Displays the entrant event catalog inside the main entrant activity.
 */
public class EntrantEventListFragment extends Fragment implements EntrantEventAdapter.OnEventActionListener {

    private EntrantEventListViewModel viewModel;
    private EntrantEventAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(EntrantEventListViewModel.class);

        SearchView searchView = view.findViewById(R.id.entrant_event_search);
        RecyclerView recyclerView = view.findViewById(R.id.entrant_event_recycler);
        progressBar = view.findViewById(R.id.entrant_event_progress);
        emptyView = view.findViewById(R.id.entrant_event_empty);

        adapter = new EntrantEventAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        observeViewModel();
        setupSearch(searchView);
    }

    private void observeViewModel() {
        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            adapter.submitList(events);
            boolean isEmpty = events == null || events.isEmpty();
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading == null) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch(@NonNull SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.filterEvents(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.filterEvents(newText);
                return true;
            }
        });
    }

    @Override
    public void onEventSelected(@NonNull Event event) {
        Toast.makeText(requireContext(), event.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onJoinWaitingList(@NonNull Event event) {
        Toast.makeText(requireContext(),
                getString(R.string.join_waiting_list_placeholder, event.getName()),
                Toast.LENGTH_SHORT).show();
    }
}
