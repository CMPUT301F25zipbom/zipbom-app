package com.example.code_zombom_app.Helpers.Users;

import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.example.code_zombom_app.Helpers.Event.Event;

import java.util.ArrayList;

/**
 * Represents an entrant using the application. Stores notification preferences,
 * and the events this entrant has interacted with.
 *
 * @author Dang Nguyen, Deng Ngut
 * @version 1.0.0, 11/3/2025
 * @see Event
 * @see Parcelable
 */
public class Entrant extends Profile {
    private boolean notificationsEnabled = true;
    private boolean lastNotificationReceived;

    /* we keep the event's ID instead of the actual
     * Event object to avoid overhead
     */
    private ArrayList<String> waitingEvents;
    private ArrayList<String> eventHistory;

    public Entrant() {}

    /**
     * MUST always call this constructor. Initialises the collections so model methods remain safe
     *
     * @param email The email address to associate this entrant with
     */
    public Entrant(String email) {
        super(email);
        this.waitingEvents = new ArrayList<>();
        this.eventHistory = new ArrayList<>();
        this.type = "Entrant";
    }

    /**
     * Convenient constructor for quickly instantiating an entrant profile.
     *
     * @param name  entrant display name
     * @param email contact email
     * @param phone optional contact phone
     */
    public Entrant(String name, String email, String phone) {
        super(name, email, phone);
        this.waitingEvents = new ArrayList<>();
        this.eventHistory = new ArrayList<>();
        this.type = "Entrant";
    }

    /**
     * Another convenient constructor for quickly instantiating an entrant profile
     * @param name          entrant display name
     * @param email         contact email
     * @param phone         optional contact phone
     * @param waitingEvents The current waiting event list of this entrant. Cannot be null
     * @param eventHistory  The current event history list of this entrant. Cannot be null
     */
    public Entrant(String name, String email, String phone,
                   @NonNull ArrayList<String> waitingEvents,
                   @NonNull ArrayList<String> eventHistory) {
        super(name, email, phone);
        this.waitingEvents = new ArrayList<>(waitingEvents); // Deep-copy
        this.eventHistory = new ArrayList<>(eventHistory);
        this.type = "Entrant";
    }

    /**
     * Copy constructor for this entrant
     *
     * @param other The other entrant to copy into this entrant
     */
    public Entrant(Entrant other) {
        super((Profile) other);
        this.waitingEvents = other.getWaitingEvents();
        this.eventHistory = other.getEventHistory();
        this.notificationsEnabled = other.areNotificationsEnabled();
        this.lastNotificationReceived = other.hasReceivedLastNotification();
    }

    /**
     * @return whether this entrant opted in to organiser/admin notifications
     */
    public boolean areNotificationsEnabled() {
        return notificationsEnabled;
    }

    /**
     * Enables or disables organiser/admin notifications for this entrant.
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * @return true if the last push/in-app notification reached this entrant
     */
    public boolean hasReceivedLastNotification() {
        return lastNotificationReceived;
    }

    /**
     * Marks that a notification for the current event context was received.
     */
    public void markNotificationReceived(boolean received) {
        this.lastNotificationReceived = received;
    }


    // region Waiting list management

    /**
     * Adds an event to the entrant's waiting list if it is not already present.
     *
     * @param event event that the entrant joined the waiting list for
     * @return true when the event id was added, false when it was already tracked
     */
    public boolean addWaitingEvent(Event event) {
        if (event == null) {
            return false;
        }
        return addWaitingEventId(event.getEventId());
    }

    /**
     * Adds an event identifier to the waiting list without requiring a full Event object.
     */
    public boolean addWaitingEventId(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return false;
        }
        if (waitingEvents.contains(eventId)) {
            return false;
        }
        return waitingEvents.add(eventId);
    }

    /**
     * Removes an event from the waiting list. Accepts either the event object or just the identifier.
     */
    public boolean removeWaitingEvent(Event event) {
        if (event == null) {
            return false;
        }
        return removeWaitingEventId(event.getEventId());
    }

    public boolean removeWaitingEventId(String eventId) {
        if (eventId == null) {
            return false;
        }
        return waitingEvents.remove(eventId);
    }

    /**
     * @return immutable view of the event identifiers this entrant is waiting on
     */
    public ArrayList<String> getWaitingEvents() {
        return new ArrayList<String>(waitingEvents);
    }

    /**
     * Checks whether the entrant is currently waiting for a specific event.
     *
     * @param eventId event identifier (may be null)
     */
    public boolean isWaitingForEvent(String eventId) {
        return eventId != null && waitingEvents.contains(eventId);
    }

    public int getWaitingEventCount() {
        return waitingEvents.size();
    }

    public void clearWaitingEvents() {
        waitingEvents.clear();
    }


    /**
     * Stores historical participation metadata for the entrant.
     */
    public void addEventToHistory(EventParticipation participation) {
        if (participation != null) {
            eventHistory.add(participation.getEventId());
        }
    }

    /**
     * Convenience helper to log a participation outcome using an Event object.
     */
    public void addEventToHistory(Event event, EventParticipation.Outcome outcome) {
        if (event == null || outcome == null) {
            return;
        }
        eventHistory.add((new EventParticipation(event.getEventId(), event.getName(), outcome)).getEventId());
    }

    /**
     * Removes a participation record. Useful when organisers delete an event.
     */
    public void removeEventFromHistory(EventParticipation participation) {
        eventHistory.remove(participation.getEventId());
    }

    /**
     * @return immutable or unmodifiable view of historic participation outcomes
     */
    public ArrayList<String> getEventHistory() {
        return new ArrayList<String>(eventHistory);
    }

    /**
     * Value object describing an entrant's outcome for a given event.
     */
    public static class EventParticipation {
        public enum Outcome {
            WAITLISTED,
            SELECTED,
            CONFIRMED,
            DECLINED,
            NOT_SELECTED
        }

        private final String eventId;
        private String eventName;
        private Outcome outcome;
        private long decisionTimestamp;

        public EventParticipation(String eventId, String eventName, Outcome outcome) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.outcome = outcome;
            this.decisionTimestamp = System.currentTimeMillis();
        }

        /**
         * @return event identifier backing this participation record
         */
        public String getEventId() {
            return eventId;
        }

        /**
         * @return human-readable event name cached at the time of logging
         */
        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        /**
         * @return current outcome/state for the entrant in this event
         */
        public Outcome getOutcome() {
            return outcome;
        }

        /**
         * Updates the outcome and resets the decision timestamp.
         */
        public void setOutcome(Outcome outcome) {
            this.outcome = outcome;
            this.decisionTimestamp = System.currentTimeMillis();
        }

        /**
         * @return epoch milliseconds of the last outcome change
         */
        public long getDecisionTimestamp() {
            return decisionTimestamp;
        }
    }
}