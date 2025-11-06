package com.example.code_zombom_app.Helpers.Event;

import com.example.code_zombom_app.Helpers.Users.Entrant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

/**
 * An Event that can be created by Organizers and participated/interacted with by the entrants
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/3/2025
 * @see Entrant
 * @see Comparable
 */
public class Event implements Comparable<Event> {
    // List of Entrants that joined the waiting list
    private final ArrayList<Entrant> waitingList;

    // List of selected Entrants from the waiting list after the lottery process
    private  final ArrayList<Entrant> chosenList;

    // List of selected Entrants that have accepted the invitation but not yet registered
    private final ArrayList<Entrant> pendingList;

    // List of selected Entrants that have registered (officially participate in) the event
    private final ArrayList<Entrant> registeredList;

    // List of all restrictions the event may have
    private final ArrayList<String> restrictions;

    /* List of categories that the event may belong to. This includes:  Sport, eSport, Food, Music,
     * and Engineering (for now)
     */
    private final ArrayList<String> categories;

    // List of guidelines for the lottery selection process that the event may have
    private final ArrayList<String> lotterySelectionGuidelines;

    // Name of the event: Must have
    private String name;

    // Created date of the event: Will be automatically assigned when an event is created
    private final Date createdDate;

    // Scheduled start date/time for an event. Null when not provided.
    private Date eventStartDate;

    // Scheduled end date/time for an event. Null when not provided.
    private Date eventEndDate;

    // Optional additional metadata exposed to entrants
    private String location;
    private int capacity;
    private String eventDateText;
    private String registrationClosesAtText;

    private static final String[] acceptedCategories = {
            "Sport", "eSport", "Food", "Music", "Engineering"
    };

    // An unique identifier of each event
    private final String eventId;

    /**
     * Private constructor for class Event. This means an event cannot be created with new Event()
     * since every event must have a name. An event will be assigned with an unique Id whenever
     * they are created successfully.
     *
     * @since 1.0.0
     */
    private Event() {
        waitingList = new ArrayList<>();
        chosenList = new ArrayList<>();
        pendingList = new ArrayList<>();
        registeredList = new ArrayList<>();
        restrictions = new ArrayList<>();
        categories = new ArrayList<>();
        lotterySelectionGuidelines = new ArrayList<>();
        createdDate = new Date(); // Get the current (created) date
        eventStartDate = null;
        eventEndDate = null;
        eventId = UUID.randomUUID().toString();
        location = "";
        capacity = 0;
        eventDateText = "";
        registrationClosesAtText = "";
    }

    /**
     * Constructor for class Event that assigns the event name during creation.
     *
     * @param name Name of the event
     * @throws IllegalArgumentException If the name is null or empty or blank or contains non-character
     *                                  symbols
     * @since 1.0.0
     */
    public Event(String name) {
        this();
        if (!ValidateName(name))
            throw new IllegalArgumentException("Invalid Name");
        this.name = name;
    }

    /**
     * Validate the name of an event,
     *
     * @param name The name of the event to validate
     * @return false if the name is null, empty, blank, or contains anything other than letters,
     *         digits, space, dash, and underscore. True otherwise
     */
    private boolean ValidateName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (name.trim().isEmpty()) return false;

        char[] cName = name.toCharArray();
        for (char c : cName) {
            if (!Character.isLetterOrDigit(c) && c != ' ' && c != '-' && c != '_')
                return false;
        }

        return true;
    }

    /**
     * Get all the Entrant that has joined the waiting list of an event.
     *
     * @return A deep-copy of the waiting list
     * @since 1.0.0
     * @see ArrayList
     * @see Entrant
     */
    public ArrayList<Entrant> getWaitingList() {
        return new ArrayList<Entrant>(this.waitingList);
    }

    /**
     * Get the current total number of entrants in the waiting list.
     *
     * @return The total number of entrants that are currently in the waiting list
     * @since 1.0.0
     */
    public int getNumberOfWaiting() {
        return this.waitingList.size();
    }

    /**
     * Add an entrant to the waiting list. An entrant can call this method to join the waiting list
     *
     * @param entrant The entrant that wishes to join the waiting list
     * @see Entrant
     * @since 1.0.0
     */
    public void joinWaitingList(Entrant entrant) {
        this.waitingList.add(entrant);
    }

    /**
     * Remove an entrant from the waiting list. An entrant can call this method if they wish
     * to leave the waiting list
     *
     * @param entrant The entrant to be removed from the waiting list
     * @see Entrant
     * @since 1.0.0
     */
    public void leaveWaitingList(Entrant entrant) {
        this.waitingList.remove(entrant);
    }

    /**
     * Add a restriction to the restriction list.
     *
     * @param restriction A new restriction to add
     * @since 1.0.0
     */
    public void addRestriction(String restriction) {
        this.restrictions.add(restriction);
    }

    /**
     * Remove a restriction from the restriction list.
     *
     * @param restriction A restriction to remove from the restriction list
     * @since 1.0.0
     */
    public void removeRestriction(String restriction) {
        this.restrictions.remove(restriction);
    }

    /**
     * Get the list of restrictions.
     *
     * @return A deep-copy of the list of restriction
     * @see ArrayList
     * @since 1.0.0
     */
    public ArrayList<String> getRestrictions() {
        return new ArrayList<String>(this.restrictions);
    }

    /**
     * Add a new category to the category list.
     *
     * @param category A new category to add to the category list
     * @throws IllegalArgumentException When the added category is not Sport, eSport, Food, Music,
     *                                  or Engineering
     * @since 1.0.0
     */
    public void addCategory(String category) {
        if (!checkCategory(category))
            throw new IllegalArgumentException("Unrecognized category");
        this.categories.add(category);
    }

    /**
     * Remove a category from the categories list. No need for exception handling in this method
     * i.e., if the removed category is not in the list or is illegal, simply do nothing
     *
     * @param category The category to be removed
     * @since 1.0.0
     */
    public void removeCategory(String category) {
        this.categories.remove(category);
    }

    /**
     * Get the categories list.
     *
     * @return A deep-copy of the categories list
     * @since 1.0.0
     */
    public ArrayList<String> getCategories() {
        return new ArrayList<String>(this.categories);
    }

    /**
     * Check if a category is in the accepted categories or not.
     *
     * @param category The category to check
     * @return true if the category is in the accepted categories for the events. False otherwise
     * @since 1.0.0
     */
    private boolean checkCategory(String category) {
        for (String c : acceptedCategories) {
            if (c.equals(category))
                return true;
        }
        return false;
    }

    /**
     * Set (edit) the name of the event.
     *
     * @param name The new name to set the name of this event
     * @throws IllegalArgumentException If the set name is null, blank, empty or contains illegal
     *                                  character
     * @since 1.0.0
     */
    public void setName(String name) {
        if (!ValidateName(name))
            throw new IllegalArgumentException("Invalid Name");
        this.name = name;
    }

    /**
     * Get the name of the event.
     *
     * @return The name of the event
     * @since 1.0.0
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the created date of the event
     *
     * @return The deep-copy of the created-date of the event
     * @see Date
     * @since 1.0.0
     */
    public Date getCreatedDate() {
        return new Date(this.createdDate.getTime());
    }

    /**
     * Set the end date of an event
     *
     * @param endDate The set end-date to an event
     * @throws IllegalArgumentException If the end-date is earlier than the created-date
     * @see Date
     * @since 1.0.0
     */
    public void setEndDate(Date endDate) {
        setEventEndDate(endDate);
    }

    /**
     * Set the start date/time for the event schedule.
     *
     * @param startDate desired start date (may be null to clear)
     * @since 1.0.0
     */
    public void setEventStartDate(Date startDate) {
        if (startDate == null) {
            this.eventStartDate = null;
        } else {
            this.eventStartDate = new Date(startDate.getTime());
        }
    }

    /**
     * Set the end date/time for the event schedule.
     *
     * @param endDate desired end date (may be null to clear)
     * @throws IllegalArgumentException when the supplied end date is earlier than the created date or today
     * @since 1.0.0
     */
    public void setEventEndDate(Date endDate) {
        if (endDate != null && (endDate.before(this.createdDate) || endDate.before(new Date()))) {
            throw new IllegalArgumentException("End-date cannot be earlier than created-date or today");
        }

        if (endDate == null) {
            this.eventEndDate = null;
        } else {
            this.eventEndDate = new Date(endDate.getTime());
        }
    }

    /**
     * Add an entrant to the chosen list (they won the lottery) if they have not in the chosen list
     *
     * @param cEntrant The chosen entrant (a winner)
     * @see Entrant
     * @since 1.0.0
     */
    public void addChosenEntrant(Entrant cEntrant) {
        if (!chosenList.contains(cEntrant))
            this.chosenList.add(cEntrant);
    }

    /**
     * Remove an entrant from the chosen list (when they decline the invitation)
     *
     * @param cEntrant The chosen entrant to be removed
     * @see Entrant
     * @since 1.0.0
     */
    public void removeChosenEntrant(Entrant cEntrant) {
        this.chosenList.remove(cEntrant);
    }

    /**
     * Return the list of chosen entrants (winners)
     *
     * @return A deep-copy of the list of chosen entrants (winners)
     * @see ArrayList
     * @see Entrant
     * @since 1.0.0
     */
    public ArrayList<Entrant> getChosenList() {
        return new ArrayList<Entrant>(this.chosenList);
    }

    /**
     * Add an entrant to the pending list (when they accept the invitation) if they have not already
     * in the list
     *
     * @param pEntrant The entrant to add to the pending list
     * @see Entrant
     * @since 1.0.0
     */
    public void addPendingEntrant(Entrant pEntrant) {
        if (!pendingList.contains(pEntrant))
            this.pendingList.add(pEntrant);
    }

    /**
     * Remove an entrant from the pending list (when they decline the invitation)
     *
     * @param pEntrant The entrant to be removed from the pending list
     * @see Entrant
     * @since 1.0.0
     */
    public void removePendingEntrant(Entrant pEntrant) {
        this.pendingList.remove(pEntrant);
    }

    /**
     * Return the list of pending entrants
     *
     * @return A deep-copy of the list of pending entrants
     * @see ArrayList
     * @see Entrant
     * @since 1.0.0
     */
    public ArrayList<Entrant> getPendingList() {
        return new ArrayList<Entrant>(this.pendingList);
    }

    /**
     * Add an entrant to the registered list (after they have registered to participate successfully)
     * if they have not already in the list
     *
     * @param rEntrant The participated entrant
     * @see Entrant
     * @since 1.0.0
     */
    public void addRegisteredEntrant(Entrant rEntrant) {
        if (!registeredList.contains(rEntrant))
            this.registeredList.add(rEntrant);
    }

    /**
     * Remove an entrant from the registered list (when they don't want to participate anymore)
     *
     * @param rEntrant The entrant to be removed from the registered list
     * @see Entrant
     * @since 1.0.0
     */
    public void removeRegisteredEntrant(Entrant rEntrant) {
        this.registeredList.remove(rEntrant);
    }

    /**
     * Return a list of registered entrants
     *
     * @return A deep-copy of the lsit of registered entrants
     * @see ArrayList
     * @see Entrant
     * @since 1.0.0
     */
    public ArrayList<Entrant> getRegisteredList() {
        return new ArrayList<Entrant>(this.registeredList);
    }

    /**
     * Add a lottery guideline to the lottery guideline list if they have not already in the list
     *
     * @param guideline A guideline to add to the lottery guideline list
     * @since 1.0.0
     */
    public void addLotteryGuideline(String guideline) {
        if (!lotterySelectionGuidelines.contains(guideline))
            this.lotterySelectionGuidelines.add(guideline);
    }

    /**
     * Remove a lottery guideline from the lottery guideline list
     *
     * @param guideline A guideline to remove from the lottery guideline list
     * @since 1.0.0
     */
    public void removeLotteryGuideline(String guideline) {
        this.lotterySelectionGuidelines.remove(guideline);
    }

    /**
     * Get the list of lottery guidelines
     *
     * @return A deep-copy of the list of lottery guidelines
     * @see ArrayList
     * @since 1.0.0
     */
    public ArrayList<String> getLotterySelectionGuidelines() {
        return new ArrayList<String>(this.lotterySelectionGuidelines);
    }

    /**
     * Get the event identifier
     *
     * @return The event's unique identifier
     * @since 1.0.0
     */
    public String getEventId() {
        return this.eventId;
    }

    /**
     * Define the natural sorting order for an event, which is by alphabetically sorting their name.
     * In case two events have the same name, sort alphabetically by their identifier.
     *
     * @param o The other event to compare this event to
     * @return Read the Comparable interface document
     * @see Comparable
     * @since 1.0.0
     */
    @Override
    public int compareTo(Event o) {
        int nameComparison = this.name.compareToIgnoreCase(o.getName());
        if (nameComparison != 0)
            return nameComparison;
        else
            return this.eventId.compareToIgnoreCase(o.getEventId());
    }

    /**
     * Updates the event location. Blank inputs are treated as no location.
     *
     * @param location user supplied location (may be null)
     */
    public void setLocation(String location) {
        if (location == null) {
            this.location = "";
        } else {
            this.location = location.trim();
        }
    }

    /**
     * @return user facing location string, or empty if not provided
     */
    public String getLocation() {
        return location;
    }

    /**
     * Updates the event capacity; values less than zero are treated as zero.
     *
     * @param capacity desired capacity
     */
    public void setCapacity(int capacity) {
        if (capacity < 0) {
            capacity = 0;
        }
        this.capacity = capacity;
    }

    /**
     * @return maximum number of confirmed entrants for the event
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @return defensive copy of the scheduled start date, or null if unset
     */
    public Date getEventStartDate() {
        if (eventStartDate == null) {
            return null;
        }
        return new Date(eventStartDate.getTime());
    }

    /**
     * @return defensive copy of the scheduled end date, or null if unset
     */
    public Date getEventEndDate() {
        if (eventEndDate == null) {
            return null;
        }
        return new Date(eventEndDate.getTime());
    }

    /**
     * Stores a human-readable event date/time string (organizer supplied).
     *
     * @param eventDate formatted date/time string
     */
    public void setEventDate(String eventDate) {
        if (eventDate == null) {
            this.eventDateText = "";
        } else {
            this.eventDateText = eventDate.trim();
        }
    }

    /**
     * @return formatted event date/time string
     */
    public String getEventDateText() {
        return eventDateText;
    }

    /**
     * Stores a human-readable registration deadline string.
     *
     * @param deadline formatted deadline value (may be null)
     */
    public void setRegistrationClosesAt(String deadline) {
        if (deadline == null) {
            this.registrationClosesAtText = "";
        } else {
            this.registrationClosesAtText = deadline.trim();
        }
    }

    /**
     * @return formatted registration closing time
     */
    public String getRegistrationClosesAtText() {
        return registrationClosesAtText;
    }

    /**
     * This class provides an additional method to sort the event by their created date from newest
     * (earliest) to oldest (most recent)
     *
     * @author Dang Nguyen
     * @version 1.0.0, 11/3/2025
     * @see Event
     * @see Comparator
     */
    public static class SortEventNewestToOldest implements Comparator<Event> {
        @Override
        public int compare(Event o1, Event o2) {
            return o2.getCreatedDate().compareTo(o1.getCreatedDate());
        }
    }

    /**
     * This class provides an additional method to sort the event by their created date from oldest
     * (most recent) to newest (earliest)
     *
     * @author Dang Nguyen
     * @version 1.0.0, 11/3/2025
     * @see Event
     * @see Comparator
     */
    public static class SortEventOldestToNewest implements Comparator<Event> {
        @Override
        public int compare(Event o1, Event o2) {
            return o1.getCreatedDate().compareTo(o2.getCreatedDate());
        }
    }

    /**
     * This class provides an additional method to sort the event by their increasing amount of
     * restriction.
     *
     * @author Dang Nguyen
     * @version 1.0.0, 11/3/2025
     * @see Event
     * @see Comparator
     */
    public static class SortEventLeastRestriction implements Comparator<Event> {
        @Override
        public int compare(Event o1, Event o2) {
            return Integer.compare(o1.getRestrictions().size(), o2.getRestrictions().size());
        }
    }

    /**
     * This class provides an additional method to sort the event by their decreasing amount of
     * people in the waiting list (most trendy to least trendy)
     *
     * @author Dang Nguyen
     * @version 1.0.0, 11/3/2025
     * @see Event
     * @see Comparator
     */
    public static class SortEventMostTrendy implements Comparator<Event> {
        @Override
        public int compare(Event o1, Event o2) {
            return Integer.compare(o2.getNumberOfWaiting(), o1.getNumberOfWaiting());
        }
    }
}
