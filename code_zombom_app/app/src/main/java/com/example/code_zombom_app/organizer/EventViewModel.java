package com.example.code_zombom_app.organizer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tejwinder Johal
 * @version 1.0
 * Just uses setEvents to put the events in a hashmap
 */
public class EventViewModel extends ViewModel {

    // Use a Map to store events: The key is the event ID (String), and the value is the event text (String).
    private final MutableLiveData<Map<String, String>> eventsMap = new MutableLiveData<>(new HashMap<>());

    // Expose the LiveData of the Map to the UI.
    public LiveData<Map<String, String>> getEventsMap() {
        return eventsMap;
    }

    /**
     * Method to set a new map of events.
     * @param newEvents
     */
    public void setEvents(Map<String, String> newEvents) {
        eventsMap.setValue(newEvents);
    }
}
