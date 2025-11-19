package com.example.code_zombom_app;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ViewModel backing the entrant event catalog screen.
 * Applies interest and availability filters on top of the Firestore event stream.
 * @author Deng Ngut
 * Date 11/04/2025
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
    /** Indicates when a waitlist Firestore transaction is running so the UI can disable controls. */
    private final MutableLiveData<Boolean> joinInProgress = new MutableLiveData<>(false);
    /** One-off success/error message that the fragment shows via a toast/snackbar. */
    private final MutableLiveData<String> joinStatusMessage = new MutableLiveData<>(null);

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerRegistration;

    private String currentQuery = "";
    private FilterSortState currentFilterSortState = new FilterSortState();
    private String entrantEmail;

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
     * @return live updates describing whether a join attempt is currently executing.
     */
    public LiveData<Boolean> isJoinInProgress() {
        return joinInProgress;
    }

    /**
     * @return single-fire payload describing the latest join success/error outcome.
     */
    public LiveData<String> getJoinStatusMessage() {
        return joinStatusMessage;
    }

    /** Clears the most recent join status so it will not be re-delivered to observers. */
    public void clearJoinStatusMessage() {
        joinStatusMessage.setValue(null);
    }

    public void filterEvents(String query) {
        currentQuery = query != null ? query : "";
        recomputeFilteredEvents();
    }

    /**
     * Replaces the active filter state with the provided values.
     */
    public void setFilterSortState(FilterSortState state) {
        currentFilterSortState = FilterSortState.copyOf(state);
        recomputeFilteredEvents();
    }

    public FilterSortState getFilterSortState() {
        return FilterSortState.copyOf(currentFilterSortState);
    }

    /**
     * Shares the signed-in entrant identifier with the ViewModel so join operations know
     * which email address to persist in Firestore.
     *
     * @param email address supplied by {@link com.example.code_zombom_app.Entrant.EntrantMainActivity}
     */
    public void setEntrantEmail(@Nullable String email) {
        entrantEmail = email;
    }

    /**
     * Attempts to add the current entrant to the supplied event waitlist. Capacity is enforced by:
     * <ol>
     *     <li>Ensuring the entrant email is not already listed.</li>
     *     <li>Checking the "Max People" field before persisting the update.</li>
     * </ol>
     * If those constraints pass a transaction updates the event document's "Entrants" array.
     */
    public void joinEvent(@NonNull Event event) {
        if (entrantEmail == null || entrantEmail.trim().isEmpty()) {
            joinStatusMessage.setValue("Please sign in before joining an event.");
            return;
        }

        String documentId = event.getFirestoreDocumentId();
        if (documentId == null || documentId.trim().isEmpty()) {
            joinStatusMessage.setValue("Cannot join this event right now.");
            return;
        }

        final String normalizedEmail = entrantEmail.trim();
        joinInProgress.setValue(true);

        DocumentReference docRef = firestore.collection("Events").document(documentId);
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Read the freshest snapshot inside the transaction so the limit check is accurate
                    DocumentSnapshot snapshot = transaction.get(docRef);

                    List<String> entrants = extractEntrantEmails(snapshot.get("Entrants"));
                    if (entrants.contains(normalizedEmail)) {
                        throw new JoinException("You have already joined this waiting list.");
                    }

                    int waitlistMaximum = extractMaxPeopleLimit(snapshot.get("Max People"));
                    if (waitlistMaximum > 0 && entrants.size() >= waitlistMaximum) {
                        throw new JoinException("This waiting list is full.");
                    }

                    ArrayList<String> updatedEntrants = new ArrayList<>(entrants);
                    updatedEntrants.add(normalizedEmail);
                    transaction.update(docRef, "Entrants", updatedEntrants);
                    return null;
                })
                .addOnSuccessListener(ignored -> {
                    String eventName = event.getName() == null ? "event" : event.getName();
                    joinStatusMessage.setValue("Joined " + eventName + " waiting list.");
                })
                .addOnFailureListener(e -> {
                    if (e instanceof JoinException) {
                        joinStatusMessage.setValue(e.getMessage());
                    } else {
                        joinStatusMessage.setValue("Couldn't join the waiting list. Please try again.");
                        Log.e(TAG, "Failed to join event " + documentId, e);
                    }
                })
                .addOnCompleteListener(task -> joinInProgress.setValue(false));
    }

    /**
     * Removes the signed-in entrant from the selected event waitlist if present.
     */
    public void leaveEvent(@NonNull Event event) {
        if (entrantEmail == null || entrantEmail.trim().isEmpty()) {
            joinStatusMessage.setValue("Please sign in before leaving an event.");
            return;
        }

        String documentId = event.getFirestoreDocumentId();
        if (documentId == null || documentId.trim().isEmpty()) {
            joinStatusMessage.setValue("Cannot leave this event right now.");
            return;
        }

        final String normalizedEmail = entrantEmail.trim();
        joinInProgress.setValue(true);

        DocumentReference docRef = firestore.collection("Events").document(documentId);
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);

                    List<String> entrants = extractEntrantEmails(snapshot.get("Entrants"));
                    if (!entrants.contains(normalizedEmail)) {
                        throw new LeaveException("You are not on this waiting list.");
                    }

                    ArrayList<String> updatedEntrants = new ArrayList<>(entrants);
                    updatedEntrants.remove(normalizedEmail);
                    transaction.update(docRef, "Entrants", updatedEntrants);
                    return null;
                })
                .addOnSuccessListener(ignored -> {
                    String eventName = event.getName() == null ? "event" : event.getName();
                    joinStatusMessage.setValue("Left " + eventName + " waiting list.");
                })
                .addOnFailureListener(e -> {
                    if (e instanceof LeaveException) {
                        joinStatusMessage.setValue(e.getMessage());
                    } else {
                        joinStatusMessage.setValue("Couldn't leave the waiting list. Please try again.");
                        Log.e(TAG, "Failed to leave event " + documentId, e);
                    }
                })
                .addOnCompleteListener(task -> joinInProgress.setValue(false));
    }

    /**
     * Subscribes to Firestore updates and keeps the view model caches synchronized.
     */
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

    /**
     * Applies interest, availability, and query filters to the supplied list.
     */
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

    /**
     * Checks whether the event overlaps the user-selected availability window.
     */
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

    /**
     * Converts a Firestore document to the domain {@link Event} instance.
     */
    private Event mapDocumentToEvent(DocumentSnapshot snapshot) {
        // Accept organiser documents that use lowercase or underscored field names for "name".
        String name = getStringField(snapshot, "Name", "Event Name");
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
        // Keep track of the originating Firestore key so we can write back when joining
        event.setFirestoreDocumentId(snapshot.getId());

        // Keep the waiting count in sync with whatever Entrants array Firestore stores.
        List<String> entrantsFromSnapshot = extractEntrantEmails(getField(snapshot, "Entrants"));
        event.setWaitingEntrantCount(entrantsFromSnapshot.size());

        // Genre/category field may be authored as either "Genre" or "Categories".
        Object rawGenre = getField(snapshot, "Genre", "Categories");
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

        // Location may be called "Location" or "Event Location" depending on the screen.
        String location = getStringField(snapshot, "Location", "Event Location");
        if (location != null && !location.trim().isEmpty()) {
            try {
                event.setLocation(location);
            } catch (IllegalArgumentException ignored) {
                event.addRestriction("Location: " + location);
            }
        }

        // Various organiser screens use different casing for the capacity field.
        String maxPeople = getStringField(snapshot, "Max People", "MaxPeople", "max_people", "Capacity");
        if (maxPeople != null) {
            try {
                int capacity = Integer.parseInt(maxPeople);
                event.setCapacity(capacity);
            } catch (NumberFormatException ignored) {
                // leave default capacity
            }
        }

        // Some organiser builds store the event date under "Date" while others use "Event Date".
        String eventDate = getStringField(snapshot, "Date", "Event Date");
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }

        // Same tolerance for the registration deadline label.
        String deadline = getStringField(snapshot, "Deadline", "Registration Deadline");
        if (deadline != null) {
            event.setRegistrationClosesAt(deadline);
        }

        String description = getStringField(snapshot, "Description", "Event Description");
        if (description != null) {
            event.setDescription(description);
        }

        // Start/end timestamps also come in a variety of keys, so normalise them to a single value.
        Date startDate = extractDate(getField(snapshot,
                "StartDate", "Start", "startDate", "Start_Time", "EventStart"));
        if (startDate == null) {
            startDate = extractDate(eventDate);
        }
        if (startDate != null) {
            event.setEventStartDate(startDate);
        }

        Date endDate = extractDate(getField(snapshot,
                "EndDate", "End", "endDate", "End_Time", "EventEnd"));
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

    /**
     * Fetches the value of the first matching field regardless of casing/spacing differences.
     *
     * @param snapshot Firestore document to inspect
     * @param candidateKeys list of possible field labels (case insensitive)
     * @return the stored value, or null when not present
     */
    @Nullable
    private Object getField(@NonNull DocumentSnapshot snapshot, String... candidateKeys) {
        Map<String, Object> data = snapshot.getData();
        if (data == null || data.isEmpty()) {
            return null;
        }

        Set<String> normalizedKeys = normalizeCandidateKeys(candidateKeys);
        if (normalizedKeys.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (normalizedKeys.contains(normalizeKey(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Convenience wrapper around {@link #getField(DocumentSnapshot, String...)} that ensures
     * the value is returned as a string.
     *
     * @param snapshot document being mapped into an Event
     * @param candidateKeys acceptable key spellings for the target property
     * @return string content when present, otherwise {@code null}
     */
    @Nullable
    private String getStringField(@NonNull DocumentSnapshot snapshot, String... candidateKeys) {
        Object value = getField(snapshot, candidateKeys);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return String.valueOf(value);
    }

    /**
     * Normalises the list of candidate keys into the canonical format used for lookups.
     */
    private Set<String> normalizeCandidateKeys(String... candidateKeys) {
        Set<String> normalized = new HashSet<>();
        if (candidateKeys == null) {
            return normalized;
        }
        for (String key : candidateKeys) {
            String normalizedKey = normalizeKey(key);
            if (!normalizedKey.isEmpty()) {
                normalized.add(normalizedKey);
            }
        }
        return normalized;
    }

    /**
     * Converts a raw Firestore field name into its canonical comparison form
     * (lowercase alphanumeric, no whitespace/punctuation).
     */
    private String normalizeKey(@Nullable String rawKey) {
        if (rawKey == null) {
            return "";
        }
        return rawKey.toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9]", "");
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

    /**
     * Safely parses the Firestore "Entrants" field into a mutable list of email strings.
     */
    private List<String> extractEntrantEmails(@Nullable Object rawValue) {
        if (rawValue == null) {
            return new ArrayList<>();
        }

        if (!(rawValue instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<String> entrants = new ArrayList<>();
        for (Object entry : (List<?>) rawValue) {
            if (entry instanceof String) {
                entrants.add((String) entry);
            }
        }
        return entrants;
    }

    /**
     * Parses the "Max People" field while tolerating missing/malformed entries.
     *
     * @param rawValue Firestore field that may be null, String, or Number
     * @return waitlist capacity or -1 when unlimited/unknown
     */
    private int extractMaxPeopleLimit(@Nullable Object rawValue) {
        if (rawValue instanceof Number) {
            return ((Number) rawValue).intValue();
        }

        if (rawValue instanceof String) {
            try {
                return Integer.parseInt(((String) rawValue).trim());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        return -1;
    }

    /**
     * Lightweight wrapper so user-facing join errors can be surfaced without logging stack traces.
     */
    private static class JoinException extends RuntimeException {
        JoinException(String message) {
            super(message);
        }
    }

    /**
     * Lightweight wrapper for leave failures so user-facing errors don't spam stack traces.
     */
    private static class LeaveException extends RuntimeException {
        LeaveException(String message) {
            super(message);
        }
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
