package com.example.code_zombom_app.Helpers.Filter;

import com.example.code_zombom_app.Helpers.Event.Event;

import java.util.Date;

/**
 * A class that holds the filter options for an event.
 *
 * @author Dang Nguyen
 * @version 11/25/2025
 * @see com.example.code_zombom_app.Helpers.Event.Event
 */
public class EventFilter {
    private String filterGenre;
    private Date filterStartDate;
    private Date filterEndDate;

    public EventFilter() {
        filterGenre = null;
        filterStartDate = null;
        filterEndDate = null;
    }

    public void setFilterGenre(String genre) {
        filterGenre = genre;
    }

    public String getFilterGenre() {
        return filterGenre;
    }

    public void setFilterStartDate(Date start) {
        filterStartDate = start;
    }

    public Date getFilterStartDate() {
        return filterStartDate;
    }

    public void setFilterEndDate(Date end) {
        filterEndDate = end;
    }

    public Date getFilterEndDate() {
        return filterEndDate;
    }

    public void reset() {
        filterGenre = null;
        filterStartDate = null;
        filterEndDate = null;
    }

    /**
     * Check if an event pass through all of the filter's criteria
     *
     * @param event The event to check
     * @return true If the event pass all of the criteria, false otherwise
     * @see com.example.code_zombom_app.Helpers.Event.Event
     */
    public boolean passFilter(Event event) {
        return (filterGenre == null || filterGenre.equals(event.getGenre()))
                && (filterStartDate == null || filterStartDate.before(event.getEventEndDate()))
                && (filterEndDate == null || filterEndDate.after(event.getEventStartDate()));
    }


}
