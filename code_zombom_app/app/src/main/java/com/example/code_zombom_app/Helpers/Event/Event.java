package com.example.code_zombom_app.Helpers.Event;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
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
@IgnoreExtraProperties
public class Event implements Comparable<Event> {
    // List of Entrants' email addresses that joined the waiting list
    private ArrayList<String> waitingList;
    private int waitingEntrantCount;

    // List of selected Entrants's email addresses from the waiting list after the lottery process
    private ArrayList<String> chosenList;

    // List of selected Entrant's email addresses that have accepted the invitation but not yet registered
    private ArrayList<String> pendingList;

    // List of selected Entrant's email addresses that have registered (officially participate in) the event
    private ArrayList<String> registeredList;
    // List of selected Entrant's email addresses that have canceled
    private ArrayList<String> cancelledList;


    // List of all restrictions the event may have
    private ArrayList<String> restrictions;

    // List of guidelines for the lottery selection process that the event may have
    private ArrayList<String> lotterySelectionGuidelines;


    private ArrayList<String> lotteryWinners;

    // Name of the event: MUST HAVE
    private String name;

    // Created date of the event: Will be automatically assigned when an event is created
    private Date createdDate;

    // Scheduled start date/time for an event. Null when not provided.
    private Date eventStartDate;

    // Scheduled end date/time for an event. Null when not provided.
    private Date eventEndDate;

    // Optional additional metadata exposed to entrants
    private Location location;

    // Set the maximum number of entrants that can join the event
    private int capacity;

    // Optional limit for how many entrants can join the waitlist (0 = unlimited)
    private int waitlistLimit;
    private String eventDateText;
    private String registrationClosesAtText;
    private String description;

    // URL of the uploaded event poster stored in Firebase Storage
    private String posterUrl;

    /**
     * NOTE: To make our life easier I have deleted the attribute categories. From now on this define
     * the genre of an event and each event has ONLY ONE genre.
     */
    private String genre;
    private int maxEntrants;
    // Marks whether the organiser has run the lottery draw
    private boolean drawComplete;
    // Timestamp (ms since epoch) when the draw completed; 0 when not set
    private long drawTimestamp;

    /* Expand this if you want to add more category */
    private static final String[] acceptedCategories = {
            "Sport", "eSport", "Food", "Music", "Engineering"
    };

    // A unique identifier of each event
    private String eventId;

    // Store the QR code that represents the unique id of an event (Base64-encoded PNG)
    private String eventIdQRcode;

    /**
     * Flag to control QR code generation in unit tests.
     * When false, QR code generation is skipped to avoid Android SDK dependencies in JVM tests.
     * Defaults to true for production behavior.
     */
    private static boolean qrCodeGenerationEnabled = true;

    /**
     * Sets whether QR code generation is enabled.
     * This is primarily used in unit tests to avoid Android SDK dependencies.
     *
     * @param enabled true to enable QR code generation (default), false to disable
     */
    public static void setQrCodeGenerationEnabled(boolean enabled) {
        qrCodeGenerationEnabled = enabled;
    }

    /**
     * Public no-arg constructor required by Firestore.
     * We also initialize sensible defaults here so objects created in code are usable.
     */
    public Event() {
        name = "";
        waitingList = new ArrayList<>();
        chosenList = new ArrayList<>();
        pendingList = new ArrayList<>();
        registeredList = new ArrayList<>();
        restrictions = new ArrayList<>();
        lotterySelectionGuidelines = new ArrayList<>();
        cancelledList = new ArrayList<>();
        lotteryWinners = new ArrayList<>();
        createdDate = new Date(); // Get the current (created) date
        eventStartDate = null;
        eventEndDate = null;
        eventId = UUID.randomUUID().toString();
        location = null;
        capacity = 0;
        waitlistLimit = 0;
        description = "";
        posterUrl = "";
        genre = "";
        maxEntrants = 0;
        drawComplete = false;
        drawTimestamp = 0L;
        waitingEntrantCount = 0;

        // Generate QR code only if enabled (disabled in unit tests to avoid Android SDK dependencies)
        if (qrCodeGenerationEnabled) {
            try {
                generateQRcode();
            } catch (WriterException e) {
                eventIdQRcode = null;
            }
        } else {
            eventIdQRcode = null;
        }
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
        if (!validateName(name))
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
    private boolean validateName(String name) {
        if (name == null) return false;

        String trimmed = name.trim();
        return !trimmed.isEmpty();
    }

    /**
     * Get all the Entrant that has joined the waiting list of an event.
     *
     * @return A deep-copy of the waiting list
     * @since 1.0.0
     * @see ArrayList
     * @see Entrant
     */
    public ArrayList<String> getWaitingList() {
        return new ArrayList<>(this.waitingList);
    }

    /**
     * Firestore setter for waiting list (needed if using automatic mapping).
     */
    @SuppressWarnings("unused")
    public void setWaitingList(ArrayList<String> waitingList) {
        this.waitingList = (waitingList == null) ? new ArrayList<>() : waitingList;
    }

    /**
     * Get the current total number of entrants in the waiting list.
     * We mark this as @Exclude so Firestore won't try to store this computed property directly.
     *
     * @return The total number of entrants that are currently in the waiting list
     * @since 1.0.0
     */
    @Exclude
    public int getNumberOfWaiting() {
        return this.waitingList.size();
    }

    /**
     * Allows Firestore mapping code to set the waitlist size even when only email addresses are known.
     *
     * @param count number of entrants currently waiting
     */
    public void setWaitingEntrantCount(int count) {
        waitingEntrantCount = Math.max(0, count);
    }

    /**
     * Add an entrant to the waiting list. An entrant can call this method to join the waiting list
     *
     * @param entrant The entrant's email address that wishes to join the waiting list
     * @throws RuntimeException If the number of people in the waiting list reached is maximum
     * @see Entrant
     * @since 1.0.0
     */
    public void joinWaitingList(String entrant) {
        if (waitingEntrantCount == waitlistLimit)
            throw new RuntimeException("Wait list is full");

        if (!waitingList.contains(entrant)) {
            this.waitingList.add(entrant);
            waitingEntrantCount++;
        }
    }

    /**
     * Remove an entrant from the waiting list. An entrant can call this method if they wish
     * to leave the waiting list
     *
     * @param entrant The entrant's email address to be removed from the waiting list
     * @see Entrant
     * @since 1.0.0
     */
    public void leaveWaitingList(String entrant) {
        this.waitingList.remove(entrant);
        waitingEntrantCount--;
    }

    /**
     * Check if an entrant is in the waiting list.
     *
     * @param entrant The entrant's email address to check
     * @return true if the entrant is in the waiting list, false otherwise
     */
    public boolean isInWaitingList(String entrant) {
        return waitingList.contains(entrant);
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
        return new ArrayList<>(this.restrictions);
    }

    @SuppressWarnings("unused")
    public void setRestrictions(ArrayList<String> restrictions) {
        this.restrictions = (restrictions == null) ? new ArrayList<>() : restrictions;
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
            if (c.equalsIgnoreCase(category))
                return true;
        }
        return false;
    }

    /**
     * Returns the canonical accepted category string when input matches (case-insensitive),
     * otherwise returns the original input.
     */
    private String normalizeCategory(String category) {
        for (String c : acceptedCategories) {
            if (c.equalsIgnoreCase(category)) {
                return c;
            }
        }
        return category;
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
        if (!validateName(name))
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        // Optional: enforce checkCategory/normalizeCategory here if you want.
        this.genre = genre;
    }

    public int getMaxEntrants() {
        return maxEntrants;
    }

    public void setMaxEntrants(int maxEntrants) {
        if (maxEntrants < 0) {
            maxEntrants = 0;
        }
        this.maxEntrants = maxEntrants;
    }

    /**
     * Get the created date of the event
     *
     * @return A deep-copy of the created-date of the event (or null if unset)
     * @see Date
     * @since 1.0.0
     */
    public Date getCreatedDate() {
        if (this.createdDate == null) {
            return null;
        }
        return new Date(this.createdDate.getTime());
    }

    @SuppressWarnings("unused")
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
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
     * @since 1.0.0
     */
    public void setEventEndDate(Date endDate) {
        if (endDate == null) {
            this.eventEndDate = null;
        } else {
            this.eventEndDate = new Date(endDate.getTime());
        }
    }

    /**
     * Add an entrant to the chosen list (they won the lottery) if they have not in the chosen list
     *
     * @param cEntrant The chosen entrant's email address (a winner)
     * @see Entrant
     * @since 1.0.0
     */
    public void addChosenEntrant(String cEntrant) {
        if (!chosenList.contains(cEntrant))
            this.chosenList.add(cEntrant);
    }

    /**
     * Remove an entrant from the chosen list (when they decline the invitation)
     *
     * @param cEntrant The chosen entrant's email address to be removed
     * @see Entrant
     * @since 1.0.0
     */
    public void removeChosenEntrant(String cEntrant) {
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
    public ArrayList<String> getChosenList() {
        return new ArrayList<>(this.chosenList);
    }

    @SuppressWarnings("unused")
    public void setChosenList(ArrayList<String> chosenList) {
        this.chosenList = (chosenList == null) ? new ArrayList<>() : chosenList;
    }

    /**
     * Add an entrant's email address to the pending list (when they accept the invitation) if they have not already
     * in the list
     *
     * @param pEntrant The entrant to add to the pending list
     * @see Entrant
     * @since 1.0.0
     */
    public void addPendingEntrant(String pEntrant) {
        if (!pendingList.contains(pEntrant))
            this.pendingList.add(pEntrant);
    }

    /**
     * Remove an entrant from the pending list (when they decline the invitation)
     *
     * @param pEntrant The entrant's email address to be removed from the pending list
     * @see Entrant
     * @since 1.0.0
     */
    public void removePendingEntrant(String pEntrant) {
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
    public ArrayList<String> getPendingList() {
        return new ArrayList<>(this.pendingList);
    }

    @SuppressWarnings("unused")
    public void setPendingList(ArrayList<String> pendingList) {
        this.pendingList = (pendingList == null) ? new ArrayList<>() : pendingList;
    }

    /**
     * Add an entrant to the registered list (after they have registered to participate successfully)
     * if they have not already in the list
     *
     * @param rEntrant The participated entrant's email address
     * @see Entrant
     * @since 1.0.0
     */
    public void addRegisteredEntrant(String rEntrant) {
        if (!registeredList.contains(rEntrant))
            this.registeredList.add(rEntrant);
    }

    /**
     * Remove an entrant from the registered list (when they don't want to participate anymore)
     *
     * @param rEntrant The entrant's email address to be removed from the registered list
     * @see Entrant
     * @since 1.0.0
     */
    public void removeRegisteredEntrant(String rEntrant) {
        this.registeredList.remove(rEntrant);
    }

    /**
     * Return a list of registered entrants
     *
     * @return A deep-copy of the list of registered entrants
     * @see ArrayList
     * @see Entrant
     * @since 1.0.0
     */
    public ArrayList<String> getRegisteredList() {
        return new ArrayList<>(this.registeredList);
    }

    @SuppressWarnings("unused")
    public void setRegisteredList(ArrayList<String> registeredList) {
        this.registeredList = (registeredList == null) ? new ArrayList<>() : registeredList;
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
        return new ArrayList<>(this.lotterySelectionGuidelines);
    }

    @SuppressWarnings("unused")
    public void setLotterySelectionGuidelines(ArrayList<String> lotterySelectionGuidelines) {
        this.lotterySelectionGuidelines =
                (lotterySelectionGuidelines == null) ? new ArrayList<>() : lotterySelectionGuidelines;
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

    @SuppressWarnings("unused")
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public ArrayList<String> getLotteryWinners() {
        return lotteryWinners;
    }

    public void setLotteryWinners(ArrayList<String> lotteryWinners) {
        this.lotteryWinners = (lotteryWinners == null) ? new ArrayList<>() : lotteryWinners;
    }

    public ArrayList<String> getCancelledList() {
        return cancelledList;
    }

    public void setCancelledList(ArrayList<String> cancelledList) {
        this.cancelledList = (cancelledList == null) ? new ArrayList<>() : cancelledList;
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
     * Updates the event location. Null inputs are treated as no location.
     *
     * @param location The location to add to an event
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * @return The event's location
     */
    public Location getLocation() {
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
     * Updates the waitlist limit; values less than zero are treated as zero.
     *
     * @param limit desired waitlist maximum
     */
    public void setWaitlistLimit(int limit) {
        if (limit < 0) {
            limit = 0;
        }
        this.waitlistLimit = limit;
    }

    /**
     * @return maximum number of entrants allowed on the waitlist (negative means unlimited)
     */
    public int getWaitlistLimit() {
        return waitlistLimit;
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
     * Stores the organiser-authored description text.
     */
    public void setDescription(String description) {
        if (description == null) {
            this.description = "";
        } else {
            this.description = description.trim();
        }
    }

    /**
     * @return organiser-authored description text (may be empty)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Stores the publicly accessible URL to the uploaded event poster.
     *
     * @param posterUrl download URL of the poster image (may be null)
     */
    public void setPosterUrl(String posterUrl) {
        if (posterUrl == null) {
            this.posterUrl = "";
        } else {
            this.posterUrl = posterUrl.trim();
        }
    }

    /**
     * @return publicly accessible URL to the uploaded poster image (empty when not set)
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    public void setDrawComplete(boolean drawComplete) {
        this.drawComplete = drawComplete;
    }

    public boolean isDrawComplete() {
        return drawComplete;
    }

    public void setDrawTimestamp(long drawTimestamp) {
        this.drawTimestamp = drawTimestamp;
    }

    public long getDrawTimestamp() {
        return drawTimestamp;
    }

    /**
     * @return The accepted categories
     */
    public static String[] getAcceptedCategories() {
        return acceptedCategories;
    }

    /**
     * @return The eventId's QR code (Base64-encoded PNG)
     */
    public String getEventIdQRcode() {
        return eventIdQRcode;
    }

    @SuppressWarnings("unused")
    public void setEventIdQRcode(String eventIdQRcode) {
        this.eventIdQRcode = eventIdQRcode;
    }

    /**
     * @return The Bitmap for the eventId's QR code (not serializable to Firestore)
     */
    @Exclude
    public Bitmap getEventIdBitmap() {
        if (eventIdQRcode == null || eventIdQRcode.isEmpty()) {
            return null;
        }
        byte[] data = Base64.decode(eventIdQRcode, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    /**
     * Generate a QR code for the event's id and store it as a String
     *
     * @throws WriterException If the QR code generation fails
     */
    private void generateQRcode() throws WriterException {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.encodeBitmap(
                eventId,
                com.google.zxing.BarcodeFormat.QR_CODE,
                200,
                200
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();

        eventIdQRcode = Base64.encodeToString(bytes, Base64.DEFAULT);
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
            Date d1 = o1.getCreatedDate();
            Date d2 = o2.getCreatedDate();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
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
            Date d1 = o1.getCreatedDate();
            Date d2 = o2.getCreatedDate();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
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
