package com.example.code_zombom_app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an entrant using the application. Stores profile data, notification preferences,
 * and the events this entrant has interacted with.
 */
public class Entrant implements Parcelable {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String deviceId;
    private boolean notificationsEnabled = true;
    private boolean lastNotificationReceived;

    private final ArrayList<Event> waitingEvents;
    private final ArrayList<EventParticipation> eventHistory;

    /**
     * No-arg constructor required for Firestore/data binding.
     * Initialises the collections so model methods remain safe.
     */
    public Entrant() {
        this.waitingEvents = new ArrayList<>();
        this.eventHistory = new ArrayList<>();
    }

    /**
     * Convenient constructor for quickly instantiating an entrant profile.
     *
     * @param id    unique identifier for persistence
     * @param name  entrant display name
     * @param email contact email
     * @param phone optional contact phone
     */
    public Entrant(String id, String name, String email, String phone) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    protected Entrant(Parcel in) {
        id = in.readString();
        name = in.readString();
        email = in.readString();
        phone = in.readString();
        deviceId = in.readString();
        notificationsEnabled = in.readByte() != 0;
        lastNotificationReceived = in.readByte() != 0;
        waitingEvents = new ArrayList<>();
        in.readTypedList(waitingEvents, Event.CREATOR);
        eventHistory = new ArrayList<>();
        in.readTypedList(eventHistory, EventParticipation.CREATOR);
    }

    public static final Creator<Entrant> CREATOR = new Creator<Entrant>() {
        @Override
        public Entrant createFromParcel(Parcel in) {
            return new Entrant(in);
        }

        @Override
        public Entrant[] newArray(int size) {
            return new Entrant[size];
        }
    };

    // region Profile management

    /**
     * @return backing-store identifier (e.g. Firestore document id)
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier; typically only used when attaching Firestore IDs.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return entrant display name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the entrant's display name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return entrant email contact
     */
    public String getEmail() {
        return email;
    }

    /**
     * Updates the entrant's contact email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return entrant phone contact (optional)
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Updates the entrant's contact phone.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return device fingerprint used for passwordless identification
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Persists the device fingerprint for this entrant.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    // endregion

    // region Waiting list management

    /**
     * Adds an event to the entrant's waiting list if it is not already present.
     *
     * @param event event that the entrant joined the waiting list for
     * @return true when the event was added, false when it was already tracked
     */
    public boolean addWaitingEvent(Event event) {
        if (event == null) {
            return false;
        }
        if (waitingEvents.stream().anyMatch(existing -> existing.getId() != null
                && existing.getId().equals(event.getId()))) {
            return false;
        }
        return waitingEvents.add(event);
    }

    /**
     * Removes an event from the waiting list.
     *
     * @param event event to remove
     * @return true if the event was present, false otherwise
     */
    public boolean removeWaitingEvent(Event event) {
        return waitingEvents.remove(event);
    }

    /**
     * @return immutable view of the events this entrant is waiting on
     */
    public List<Event> getWaitingEvents() {
        return Collections.unmodifiableList(waitingEvents);
    }

    /**
     * Checks whether the entrant is currently waiting for a specific event.
     *
     * @param eventId event identifier (may be null)
     */
    public boolean isWaitingForEvent(String eventId) {
        return waitingEvents.stream().anyMatch(event -> eventId != null && eventId.equals(event.getId()));
    }

    // endregion

    // region Event history

    /**
     * Stores historical participation metadata for the entrant.
     */
    public void addEventToHistory(EventParticipation participation) {
        if (participation != null) {
            eventHistory.add(participation);
        }
    }

    /**
     * Removes a participation record. Useful when organisers delete an event.
     */
    public void removeEventFromHistory(EventParticipation participation) {
        eventHistory.remove(participation);
    }

    /**
     * @return immutable view of historic participation outcomes
     */
    public List<EventParticipation> getEventHistory() {
        return Collections.unmodifiableList(eventHistory);
    }

    // endregion

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(deviceId);
        dest.writeByte((byte) (notificationsEnabled ? 1 : 0));
        dest.writeByte((byte) (lastNotificationReceived ? 1 : 0));
        dest.writeTypedList(waitingEvents);
        dest.writeTypedList(eventHistory);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Value object describing an entrant's outcome for a given event.
     */
    public static class EventParticipation implements Parcelable {
        public enum Outcome {
            WAITLISTED,
            SELECTED,
            CONFIRMED,
            DECLINED,
            NOT_SELECTED
        }

        private final Event event;
        private Outcome outcome;
        private long decisionTimestamp;

        public EventParticipation(Event event, Outcome outcome) {
            this.event = event;
            this.outcome = outcome;
            this.decisionTimestamp = System.currentTimeMillis();
        }

        protected EventParticipation(Parcel in) {
            event = in.readParcelable(Event.class.getClassLoader());
            outcome = Outcome.valueOf(in.readString());
            decisionTimestamp = in.readLong();
        }

        public static final Creator<EventParticipation> CREATOR = new Creator<EventParticipation>() {
            @Override
            public EventParticipation createFromParcel(Parcel in) {
                return new EventParticipation(in);
            }

            @Override
            public EventParticipation[] newArray(int size) {
                return new EventParticipation[size];
            }
        };

        /**
         * @return event backing this participation record
         */
        public Event getEvent() {
            return event;
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

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(event, flags);
            dest.writeString(outcome.name());
            dest.writeLong(decisionTimestamp);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}