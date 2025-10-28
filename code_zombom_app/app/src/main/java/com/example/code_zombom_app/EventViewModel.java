package com.example.code_zombom_app;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

// Event class to hold event data
public class EventViewModel extends ViewModel {

    // Private MutableLiveData that can be modified within the ViewModel.
    private final MutableLiveData<List<String>> eventList = new MutableLiveData<>(new ArrayList<>());

    // Public Read-Only LiveData that is exposed to the UI for observation
    public LiveData<List<String>> getEventList() {
        return eventList;
    }

    // Add a new event.
    public void addEvent(String newEvent) {
        // Get the current list, add the new item, and set the new list to trigger observers.
        List<String> currentList = eventList.getValue();
        if (currentList != null) {
            currentList.add(newEvent);
            eventList.setValue(currentList);
        }
    }
}