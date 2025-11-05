package com.example.code_zombom_app;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * ViewModel backing the entrant event catalog screen.
 * <p>
 * This will eventually delegate to a repository backed by Firestore. For now, it provides
 * placeholder data to unblock the UI.
 * </p>
 */
public class EntrantEventListViewModel extends ViewModel {

    private final MutableLiveData<List<Event>> allEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Event>> filteredEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerRegistration;
    private String currentQuery = "";

    public EntrantEventListViewModel() {
        observeEventsFromFirestore();
    }

    public LiveData<List<Event>> getEvents() {
        return filteredEvents;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Filters the list to contain items that match the query on either name or category.
     */
    public void filterEvents(String query) {
        currentQuery = query != null ? query : "";
        List<Event> events = allEvents.getValue();
        if (events == null) {
            filteredEvents.setValue(new ArrayList<>());
            return;
        }

        if (query == null || query.trim().isEmpty()) {
            filteredEvents.setValue(new ArrayList<>(events));
            return;
        }

        final String searchable = query.trim().toLowerCase(Locale.getDefault());

        List<Event> result = events.stream()
                .filter(event -> event.getName().toLowerCase(Locale.getDefault()).contains(searchable)
                        || event.getCategories().stream()
                        .anyMatch(cat -> cat.toLowerCase(Locale.getDefault()).contains(searchable)))
                .collect(Collectors.toList());
        filteredEvents.setValue(result);
    }

    /**
     * Subscribes to Firestore so the entrant event list reacts to organizer changes in real time.
     */
    private void observeEventsFromFirestore() {
        loading.setValue(true);
        listenerRegistration = firestore.collection("Events")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            errorMessage.setValue(error.getMessage());
                            loading.setValue(false);
                            return;
                        }

                        if (value == null) {
                            allEvents.setValue(new ArrayList<>());
                            filteredEvents.setValue(new ArrayList<>());
                            loading.setValue(false);
                            return;
                        }

                        ArrayList<Event> events = new ArrayList<>();
                        for (DocumentSnapshot snapshot : value) {
                            Event event = mapDocumentToEvent(snapshot);
                            if (event != null) {
                                events.add(event);
                            }
                        }
                        allEvents.setValue(events);
                        reapplyFilter(events);
                        loading.setValue(false);
                    }
                });
    }

    /**
     * Applies the most recent search query against a fresh list of events.
     */
    private void reapplyFilter(List<Event> events) {
        if (currentQuery == null || currentQuery.trim().isEmpty()) {
            filteredEvents.setValue(new ArrayList<>(events));
        } else {
            filterEvents(currentQuery);
        }
    }

    /**
     * Converts a Firestore document into the {@link Event} domain object, returning {@code null}
     * when required fields are missing.
     */
    private Event mapDocumentToEvent(DocumentSnapshot snapshot) {
        String name = snapshot.getString("Name");
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Event event = new Event(name);
        event.setEventId(snapshot.getId());

        String genre = snapshot.getString("Genre");
        if (genre != null && !genre.trim().isEmpty()) {
            try {
                event.addCategory(genre);
            } catch (IllegalArgumentException ex) {
                event.addRestriction("Category: " + genre);
            }
        }

        String location = snapshot.getString("Location");
        if (location != null && !location.trim().isEmpty()) {
            try {
                event.setLocation(location);
            } catch (IllegalArgumentException ignored) {
                event.addRestriction("Location: " + location);
            }
        }

        String maxPeople = snapshot.getString("Max People");
        if (maxPeople != null) {
            try {
                int capacity = Integer.parseInt(maxPeople);
                event.setCapacity(capacity);
            } catch (NumberFormatException ignored) {
                // leave default capacity
            }
        }

        String eventDate = snapshot.getString("Date");
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }

        String deadline = snapshot.getString("Deadline");
        if (deadline != null) {
            event.setRegistrationClosesAt(deadline);
        }

        return event;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
