package com.example.code_zombom_app.organizer;

import com.google.firebase.firestore.PropertyName;

import java.util.List;

/**
 * A Plain Old Java Object (POJO) to represent a complete event document from Firestore.
 * This class includes all details for an event, including entrant lists.
 * The @PropertyName annotation is used to map Firestore field names with spaces
 * to Java variable names.
 *
 * NOT IMPLEMENTED YET JUST FOR FUTURE CLEANING UP OF OUR CODE
 */
public class Event {

    // It's good practice to keep field names consistent with your database.
    // However, Java naming conventions prefer camelCase. @PropertyName solves this.
    private String Name;
    private String Date;
    private String Deadline;
    private String Genre;
    private String Location;

    // Use @PropertyName to map Firestore fields with spaces or different capitalization
    // to your Java variables.
    @PropertyName("Max People")
    private String maxPeople;

    @PropertyName("Wait List Maximum")
    private String waitListMaximum;

    @PropertyName("Entrants")
    private List<String> entrants;

    @PropertyName("Accepted Entrants")
    private List<String> acceptedEntrants;

    @PropertyName("Cancelled Entrants")
    private List<String> cancelledEntrants;

    // A no-argument constructor is required for Firestore's automatic data mapping
    public Event() {
    }

    // --- GETTERS ---

    public String getName() {
        return Name;
    }

    public String getDate() {
        return Date;
    }

    public String getDeadline() {
        return Deadline;
    }

    public String getGenre() {
        return Genre;
    }

    public String getLocation() {
        return Location;
    }

    @PropertyName("Max People")
    public String getMaxPeople() {
        return maxPeople;
    }

    @PropertyName("Wait List Maximum")
    public String getWaitListMaximum() {
        return waitListMaximum;
    }

    @PropertyName("Entrants")
    public List<String> getEntrants() {
        return entrants;
    }

    @PropertyName("Accepted Entrants")
    public List<String> getAcceptedEntrants() {
        return acceptedEntrants;
    }

    @PropertyName("Cancelled Entrants")
    public List<String> getCancelledEntrants() {
        return cancelledEntrants;
    }

    // --- SETTERS ---

    public void setName(String name) {
        this.Name = name;
    }

    public void setDate(String date) {
        this.Date = date;
    }

    public void setDeadline(String deadline) {
        this.Deadline = deadline;
    }

    public void setGenre(String genre) {
        this.Genre = genre;
    }

    public void setLocation(String location) {
        this.Location = location;
    }

    @PropertyName("Max People")
    public void setMaxPeople(String maxPeople) {
        this.maxPeople = maxPeople;
    }

    @PropertyName("Wait List Maximum")
    public void setWaitListMaximum(String waitListMaximum) {
        this.waitListMaximum = waitListMaximum;
    }

    @PropertyName("Entrants")
    public void setEntrants(List<String> entrants) {
        this.entrants = entrants;
    }

    @PropertyName("Accepted Entrants")
    public void setAcceptedEntrants(List<String> acceptedEntrants) {
        this.acceptedEntrants = acceptedEntrants;
    }

    @PropertyName("Cancelled Entrants")
    public void setCancelledEntrants(List<String> cancelledEntrants) {
        this.cancelledEntrants = cancelledEntrants;
    }
}
