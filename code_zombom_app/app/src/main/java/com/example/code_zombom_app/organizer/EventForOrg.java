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

    public String getName() { return Name; }
    public void setName(String name) { this.Name = name; }

    public String getDate() { return Date; }
    public void setDate(String date) { this.Date = date; }

    public String getDeadline() { return Deadline; }
    public void setDeadline(String deadline) { this.Deadline = deadline; }

    public String getGenre() { return Genre; }
    public void setGenre(String genre) { this.Genre = genre; }

    public Location getLocation() { return Location; }
    public void setLocation(Location location) { this.Location = location; }

    public String getDescription() { return Description; }
    public void setDescription(String description) { this.Description = description; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getMax_People() { return Max_People; }
    public void setMax_People(String max_People) { this.Max_People = max_People; }
    public Boolean getQrCodeExists() { return qrCodeExists; }

    public String getWait_List_Maximum() { return Wait_List_Maximum; }
    public void setWait_List_Maximum(String wait_List_Maximum) { this.Wait_List_Maximum = wait_List_Maximum; }

    public ArrayList<String> getEntrants() { return Entrants; }
    public void setEntrants(ArrayList<String> entrants) { this.Entrants = entrants; }

    public ArrayList<String> getCancelled_Entrants() { return Cancelled_Entrants; }
    public void setCancelled_Entrants(ArrayList<String> cancelled_Entrants) { this.Cancelled_Entrants = cancelled_Entrants; }

    public ArrayList<String> getAccepted_Entrants() { return Accepted_Entrants; }
    public void setAccepted_Entrants(ArrayList<String> accepted_Entrants) { this.Accepted_Entrants = accepted_Entrants; }

    public ArrayList<String> getLottery_Winners() { return Lottery_Winners; }
    public void setLottery_Winners(ArrayList<String> lottery_Winners) { this.Lottery_Winners = lottery_Winners; }

    public Boolean getDrawComplete() { return drawComplete; }
    public void setDrawComplete(Boolean drawComplete) { this.drawComplete = drawComplete; }

    public Long getDrawTimestamp() { return drawTimestamp; }
    public void setDrawTimestamp(Long drawTimestamp) { this.drawTimestamp = drawTimestamp; }


    // Excluded (local-only) properties

    @Exclude
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

}
