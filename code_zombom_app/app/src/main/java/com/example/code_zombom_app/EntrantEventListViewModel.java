package com.example.code_zombom_app;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * ViewModel backing the entrant event catalog screen.
 * Applies interest and availability filters on top of the Firestore event stream.
 */
public class EntrantEventListViewModel extends ViewModel {

    private static final String TAG = "EntrantEventListVM";
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "MMM d, yyyy",
            "MMM d yyyy",
            "MMMM d, yyyy",
            "MMMM d yyyy"
    };

    private final MutableLiveData<List<Event>> allEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Event>> filteredEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerRegistration;

    private String currentQuery = "";
    private FilterSortState currentFilterSortState = new FilterSortState();

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

    public void filterEvents(String query) {
        currentQuery = query != null ? query : "";
        recomputeFilteredEvents();
    }

    public void setFilterSortState(FilterSortState state) {
        currentFilterSortState = FilterSortState.copyOf(state);
        recomputeFilteredEvents();
    }

    public FilterSortState getFilterSortState() {
        return FilterSortState.copyOf(currentFilterSortState);
    }

    private void observeEventsFromFirestore() {
        loading.setValue(true);
        listenerRegistration = firestore.collection("Events")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
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
                        filteredEvents.setValue(applyAllFilters(events));
                        loading.setValue(false);
                    }
                });
    }

    private void recomputeFilteredEvents() {
        List<Event> events = allEvents.getValue();
        if (events == null) {
            filteredEvents.setValue(new ArrayList<>());
            return;
        }
        filteredEvents.setValue(applyAllFilters(events));
    }

    private List<Event> applyAllFilters(List<Event> sourceEvents) {
        if (sourceEvents == null) {
            return Collections.emptyList();
        }

        List<Event> working = new ArrayList<>(sourceEvents);

        if (currentFilterSortState.isFilterByInterests()) {
            String desiredCategory = currentFilterSortState.getSelectedInterestCategory();
            if (desiredCategory != null && !desiredCategory.trim().isEmpty()) {
                final String normalizedCategory = desiredCategory.trim().toLowerCase(Locale.getDefault());
                working = working.stream()
                        .filter(event -> event.getCategories().stream()
                                .anyMatch(cat -> cat != null
                                        && cat.trim().toLowerCase(Locale.getDefault()).equals(normalizedCategory)))
                        .collect(Collectors.toList());
            }
        }

        if (currentFilterSortState.isFilterByAvailability()) {
            working = working.stream()
                    .filter(this::matchesAvailability)
                    .collect(Collectors.toList());
        }

        final String searchable = currentQuery == null ? "" : currentQuery.trim().toLowerCase(Locale.getDefault());
        if (!searchable.isEmpty()) {
            working = working.stream()
                    .filter(event -> event.getName().toLowerCase(Locale.getDefault()).contains(searchable)
                            || event.getCategories().stream()
                            .anyMatch(cat -> cat != null
                                    && cat.toLowerCase(Locale.getDefault()).contains(searchable)))
                    .collect(Collectors.toList());
        }

        return working;
    }

    private boolean matchesAvailability(Event event) {
        if (!currentFilterSortState.isFilterByAvailability()) {
            return true;
        }

        Date filterStart = currentFilterSortState.getAvailabilityStart();
        Date filterEnd = currentFilterSortState.getAvailabilityEnd();
        Date eventStart = event.getEventStartDate();
        Date eventEnd = event.getEventEndDate();

        if (filterStart == null && filterEnd == null) {
            return true;
        }

        if (eventStart == null && eventEnd == null) {
            // Without schedule data we cannot validate availability.
            return false;
        }

        if (filterStart == null) {
            filterStart = filterEnd;
        }
        if (filterEnd == null) {
            filterEnd = filterStart;
        }

        if (eventStart == null) {
            eventStart = eventEnd;
        }
        if (eventEnd == null) {
            eventEnd = eventStart;
        }

        if (eventStart == null) {
            return false;
        }

        return !eventEnd.before(filterStart) && !eventStart.after(filterEnd);
    }

    private Event mapDocumentToEvent(DocumentSnapshot snapshot) {
        String name = snapshot.getString("Name");
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Event event;
        try {
            event = new Event(name);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Skipping event with invalid name: " + name, ex);
            return null;
        }

        Object rawGenre = snapshot.get("Genre");
        List<String> categories = extractCategories(rawGenre);
        if (!categories.isEmpty()) {
            for (String category : categories) {
                try {
                    event.addCategory(category);
                } catch (IllegalArgumentException ex) {
                    Log.w(TAG, "Unrecognized category '" + category + "' for event " + name, ex);
                    event.addRestriction("Category: " + category);
                }
            }
        } else if (rawGenre != null) {
            Log.w(TAG, "No recognized categories parsed from '" + rawGenre + "' for event " + name);
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

        Date startDate = extractDate(snapshot.get("StartDate"));
        if (startDate == null) {
            startDate = extractDate(snapshot.get("Start"));
        }
        if (startDate == null) {
            startDate = extractDate(snapshot.get("startDate"));
        }
        if (startDate == null) {
            startDate = extractDate(snapshot.get("Start_Time"));
        }
        if (startDate == null) {
            startDate = extractDate(eventDate);
        }
        if (startDate != null) {
            event.setEventStartDate(startDate);
        }

        Date endDate = extractDate(snapshot.get("EndDate"));
        if (endDate == null) {
            endDate = extractDate(snapshot.get("End"));
        }
        if (endDate == null) {
            endDate = extractDate(snapshot.get("endDate"));
        }
        if (endDate == null) {
            endDate = extractDate(snapshot.get("End_Time"));
        }
        if (endDate == null) {
            endDate = extractDate(deadline);
        }
        if (endDate != null) {
            try {
                event.setEventEndDate(endDate);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Event end date invalid for " + name + ": " + endDate, ex);
            }
        }

        return event;
    }

    private List<String> extractCategories(@Nullable Object rawGenre) {
        if (rawGenre == null) {
            return Collections.emptyList();
        }

        List<String> categories = new ArrayList<>();

        if (rawGenre instanceof String) {
            categories.addAll(parseCategories((String) rawGenre));
        } else if (rawGenre instanceof List<?>) {
            for (Object item : (List<?>) rawGenre) {
                if (item instanceof String) {
                    categories.addAll(parseCategories((String) item));
                }
            }
        }

        return categories;
    }

    private List<String> parseCategories(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }

        String[] tokens = raw.split("[,;/]");
        List<String> categories = new ArrayList<>();
        for (String token : tokens) {
            String normalized = normalizeCategory(token);
            if (normalized != null) {
                categories.add(normalized);
            }
        }
        return categories;
    }

    @Nullable
    private String normalizeCategory(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.getDefault());
        if (lower.equals("sport") || lower.equals("sports")) {
            return "Sport";
        } else if (lower.equals("esport") || lower.equals("e-sport") || lower.equals("esports") || lower.equals("e sports")) {
            return "eSport";
        } else if (lower.equals("food") || lower.equals("foods") || lower.equals("culinary")) {
            return "Food";
        } else if (lower.equals("music") || lower.equals("musical")) {
            return "Music";
        } else if (lower.equals("engineering") || lower.equals("engineer") || lower.equals("stem")) {
            return "Engineering";
        }
        return null;
    }

    @Nullable
    private Date extractDate(@Nullable Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof Date) {
            return (Date) rawValue;
        }

        if (rawValue instanceof Timestamp) {
            return ((Timestamp) rawValue).toDate();
        }

        if (rawValue instanceof String) {
            return parseDateString((String) rawValue);
        }

        return null;
    }

    @Nullable
    private Date parseDateString(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
                format.setLenient(false);
                return format.parse(trimmed);
            } catch (ParseException ignored) {
            }
        }

        Log.w(TAG, "Unable to parse date string: " + raw);
        return null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
