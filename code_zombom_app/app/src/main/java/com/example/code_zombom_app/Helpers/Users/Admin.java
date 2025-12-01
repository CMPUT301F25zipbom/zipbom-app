package com.example.code_zombom_app.Helpers.Users;

import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Represents an Admin using the application. Stores notification preferences,
 * and the events this entrant has interacted with.
 * (IS A TRANSLATED COPY OF ENTRANT)
 *
 * @author Robert Enstrom
 * @version 1.0.0, 11/29/2025
 * @see Event
 * @see Parcelable
 */

public class Admin extends Profile{
    private boolean notificationsEnabled;
    private boolean lastNotificationReceived;

    /* We keep the event's ID instead of the actual Event object to avoid overhead */
    private ArrayList<String> waitingEvents;
    private HashMap<String, Entrant.Status> eventHistory;

    public enum Status {
        WAITLISTED,
        NOT_SELECTED,
        LEAVE,
        SELECTED,
        CONFIRMED,
        REGISTERED,
        DECLINED,

    }

    public Admin() {}

    /**
     * MUST always call this constructor. Initialises the collections so model methods remain safe
     *
     * @param email The email address to associate this entrant with
     */
    public Admin(String email) {
        super(email);
        this.waitingEvents = new ArrayList<>();
        this.eventHistory = new HashMap<>();
        this.type = "Admin";
    }

    /**
     * Convenient constructor for quickly instantiating an entrant profile.
     *
     * @param name  entrant display name
     * @param email contact email
     * @param phone optional contact phone
     */
    public Admin(String name, String email, String phone) {
        super(name, email, phone);
        this.waitingEvents = new ArrayList<>();
        this.eventHistory = new HashMap<>();
        this.type = "Admin";
    }

    /**
     * Another convenient constructor for quickly instantiating an entrant profile
     * @param name          entrant display name
     * @param email         contact email
     * @param phone         optional contact phone
     * @param waitingEvents The current waiting event list of this entrant. Cannot be null
     * @param eventHistory  The current event history list of this entrant. Cannot be null
     */
    public Admin(String name, String email, String phone,
                     @NonNull ArrayList<String> waitingEvents,
                     @NonNull HashMap<String, Entrant.Status> eventHistory) {
        super(name, email, phone);
        this.waitingEvents = new ArrayList<>(waitingEvents); // Deep-copy
        this.eventHistory = new HashMap<>(eventHistory);
        this.type = "Admin";
    }

    /**
     * Copy constructor for this entrant
     *
     * @param other The other entrant to copy into this entrant
     */
    public Admin(Entrant other) {
        super((Profile) other);
        this.waitingEvents = other.getWaitingEvents();
        this.eventHistory = other.getEventHistory();
        this.notificationsEnabled = other.isNotificationEnabled();
        this.lastNotificationReceived = other.isLastNotificationReceived();
        this.type = "Admin";
    }

    /**
     * @return whether this entrant opted in to organiser/admin notifications
     */
    @PropertyName("notificationEnabled")
    public boolean isNotificationEnabled() {
        return notificationsEnabled;
    }

    /**
     * Enables or disables organiser/admin notifications for this entrant.
     */
    @PropertyName("notificationEnabled")
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * @return true if the last push/in-app notification reached this entrant
     */
    public boolean isLastNotificationReceived() {
        return lastNotificationReceived;
    }

    /**
     * Marks that a notification for the current event context was received.
     */
    public void setLastNotificationReceived(boolean received) {
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
     *
     * @param eventId The event id to add to the evnet history
     * @param status The status of that event
     */
    public void addEventToHistory(String eventId, Entrant.Status status) {
        if (eventHistory.containsKey(eventId)) {
            eventHistory.replace(eventId, status);
        } else {
            eventHistory.put(eventId, status);
        }
    }

    /**
     * Removes a participation record. Useful when organisers delete an event.
     *
     * @param eventId The event Id to be removed fron the history
     */
    public void removeEventFromHistory(String eventId) {
        eventHistory.remove(eventId);
    }

    /**
     * @return immutable or unmodifiable view of historic participation outcomes
     */
    public HashMap<String, Entrant.Status> getEventHistory() {
        return eventHistory;
    }

    /**
     * Set the event history
     *
     * @param eventHistory The event history to set
     */
    public void setEventHistory(HashMap<String, Entrant.Status> eventHistory) {
        this.eventHistory = eventHistory;
    }
}
