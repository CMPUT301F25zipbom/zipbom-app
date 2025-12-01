package com.example.code_zombom_app.organizer;

import com.example.code_zombom_app.Helpers.Location.Location;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A model class representing an event. This POJO
 * is used for direct mapping with Firestore documents.
 */
public class EventForOrg implements Serializable { // Serializable is good practice for passing objects in Bundles

    // Properties that map directly to Firestore fields
    private String Name;
    private String Date;
    private String Deadline;
    private String Genre;
    private Location Location;
    private String Description;
    private String posterUrl;
    private String Max_People;
    private String Wait_List_Maximum;
    private Boolean qrCodeExists;
    private Boolean drawComplete;
    private Long drawTimestamp;

    private ArrayList<String> Entrants;
    private ArrayList<String> Cancelled_Entrants;
    private ArrayList<String> Accepted_Entrants;
    private ArrayList<String> Lottery_Winners;


    // Properties that are NOT in Firestore
    @Exclude // This annotation tells Firestore to ignore this field
    private String eventId;

    public EventForOrg() {
        Name = "";
        Date = "";
        Deadline = "";
        Genre = "";
        Location = null;
        Description = "";
        Max_People = "0";
        Wait_List_Maximum = "0";
        qrCodeExists = false;
        Entrants = new ArrayList<>();
        Cancelled_Entrants = new ArrayList<>();
        Accepted_Entrants = new ArrayList<>();
        Lottery_Winners = new ArrayList<>();
        drawComplete = false;
        drawTimestamp = 0L;
    }


    // --- Getters and Setters ---
    // Firestore uses these to automatically populate the object

    /**
     * This method is used to get the name.
     * @return The name of the event
     */
    public String getName() { return Name; }

    /**
     * This method is used to set the name.
     * @param name
     */
    public void setName(String name) { this.Name = name; }

    /**
     * This method is used to get the date.
     * @return The date of the event
     */
    public String getDate() { return Date; }

    /**
     * This method is used to set the date.
     * @param date
     */
    public void setDate(String date) { this.Date = date; }

    /**
     * This method is used to get the deadline.
     * @return The deadline of the event
     */
    public String getDeadline() { return Deadline; }

    /**
     * This method is used to set the deadline.
     * @param deadline
     */
    public void setDeadline(String deadline) { this.Deadline = deadline; }

    /**
     * This method is used to get the genre.
     * @return The genre of the event
     */
    public String getGenre() { return Genre; }

    /**
     * This method is used to set the genre.
     * @param genre
     */
    public void setGenre(String genre) { this.Genre = genre; }

    /**
     * This method is used to get the location.
     * @return The location of the event
     */
    public Location getLocation() { return Location; }
    /**
     * This method is used to set the location.
     * @param location
     */
    public void setLocation(Location location) { this.Location = location; }

    /**
     * This method is used to get the description.
     * @return
     */
    public String getDescription() { return Description; }
    /**
     * This method is used to set the description.
     * @param description
     */
    public void setDescription(String description) { this.Description = description; }

    /**
     * This method is used to get the posterUrl.
     * @return The posterUrl of the event
     */
    public String getPosterUrl() { return posterUrl; }
    /**
     * This method is used to set the posterUrl.
     * @param posterUrl
     */
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /**
     * This method is used to get the max_People.
     * @return The max_People of the event
     */
    public String getMax_People() { return Max_People; }
    /**
     * This method is used to set the max_People.
     * @param max_People
     */
    public void setMax_People(String max_People) { this.Max_People = max_People; }

    /**
     * This method is used to get the qrCodeExists.
     * @return The qrCodeExists of the event
     */
    public Boolean getQrCodeExists() { return qrCodeExists; }

    /**
     * This method gets the Wait List Maximum.
     * @return The wait_List_Maximum of the event
     */
    public String getWait_List_Maximum() { return Wait_List_Maximum; }

    /**
     * This method is used to set the wait list maximum.
     * @param wait_List_Maximum
     */
    public void setWait_List_Maximum(String wait_List_Maximum) { this.Wait_List_Maximum = wait_List_Maximum; }

    /**
     * This method is used to get the wait list entrants
     * @return The wait list entrants
     */
    public ArrayList<String> getEntrants() { return Entrants; }
    /**
     * This method is used to set the wait list entrants.
     * @param entrants
     */
    public void setEntrants(ArrayList<String> entrants) { this.Entrants = entrants; }

    /**
     * This method is used to get the cancelled entrants.
     * @return The the cancelled entrants
     */
    public ArrayList<String> getCancelled_Entrants() { return Cancelled_Entrants; }
    /**
     * This method is used to set the cancelled entrants.
     * @param cancelled_Entrants
     */
    public void setCancelled_Entrants(ArrayList<String> cancelled_Entrants) { this.Cancelled_Entrants = cancelled_Entrants; }

    /**
     * This method is used to get the entrants who've accepted the invide.
     * @return The entrants who've accepted the invide of the event
     */
    public ArrayList<String> getAccepted_Entrants() { return Accepted_Entrants; }
    /**
     * This method is used to set the entrants who've accepted the invide.
     * @param accepted_Entrants
     */
    public void setAccepted_Entrants(ArrayList<String> accepted_Entrants) { this.Accepted_Entrants = accepted_Entrants; }

    /**
     * This method is used to get the lottery winners.
     * @return The lottery winners of the event
     */
    public ArrayList<String> getLottery_Winners() { return Lottery_Winners; }
    /**
     * This method is used to set the lottery winners.
     * @param lottery_Winners
     */
    public void setLottery_Winners(ArrayList<String> lottery_Winners) { this.Lottery_Winners = lottery_Winners; }

    /**
     * This method is used to get the draw complete.
     * @return the draw complete of the event
     */
    public Boolean getDrawComplete() { return drawComplete; }
    /**
     * This method is used to set the draw complete.
     * @param drawComplete
     */
    public void setDrawComplete(Boolean drawComplete) { this.drawComplete = drawComplete; }

    /**
     * This method is used to get the draw timestamp.
     * @return the draw timestamp of the event
     */
    public Long getDrawTimestamp() { return drawTimestamp; }
    /**
     * This method is used to set the draw timestamp.
     * @param drawTimestamp
     */
    public void setDrawTimestamp(Long drawTimestamp) { this.drawTimestamp = drawTimestamp; }


    // Excluded (local-only) properties

    /**
     * This method is used to get the eventId.
     * @return The eventId of the event
     */
    @Exclude
    public String getEventId() { return eventId; }
    /**
     * This method is used to set the eventId.
     * @param eventId
     */
    @Exclude
    public void setEventId(String eventId) { this.eventId = eventId; }

}
