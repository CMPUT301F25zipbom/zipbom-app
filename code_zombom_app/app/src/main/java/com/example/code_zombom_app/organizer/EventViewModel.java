package com.example.code_zombom_app.organizer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class EventViewModel extends ViewModel {

    // 1. Use a Map to store events: The key is the event ID (String), and the value is the event text (String).
    private final MutableLiveData<Map<String, String>> eventsMap = new MutableLiveData<>(new HashMap<>());

    // 2. Expose the LiveData of the Map to the UI.
    public LiveData<Map<String, String>> getEventsMap() {
        return eventsMap;
    }

    // 3. Method to add a new event to the map.
    public void addEvent(String eventId, String eventText) {
        Map<String, String> currentMap = eventsMap.getValue();
        if (currentMap != null) {
            // Create a new map to ensure LiveData triggers an update.
            Map<String, String> updatedMap = new HashMap<>(currentMap);
            updatedMap.put(eventId, eventText);
            eventsMap.setValue(updatedMap);
        }
    }

    // 4. Method to update an existing event in the map.
    public void updateEvent(String eventId, String newEventText) {
        Map<String, String> currentMap = eventsMap.getValue();
        // Check if the map is not null and contains the event to be updated.
        if (currentMap != null && currentMap.containsKey(eventId)) {
            Map<String, String> updatedMap = new HashMap<>(currentMap);
            updatedMap.put(eventId, newEventText); // Replace the old text with the new one.
            eventsMap.setValue(updatedMap);
        }
    }

    // 5. Method to remove an event from the map using its ID.
    public void removeEvent(String eventId) {
        Map<String, String> currentMap = eventsMap.getValue();
        if (currentMap != null && currentMap.containsKey(eventId)) {
            Map<String, String> updatedMap = new HashMap<>(currentMap);
            updatedMap.remove(eventId);
            eventsMap.setValue(updatedMap);
        }
    }

    // 6. Helper method to replace the entire map at once (useful for initial load from Firestore).
    public void setEvents(Map<String, String> newEvents) {
        eventsMap.setValue(newEvents);
    }
}
