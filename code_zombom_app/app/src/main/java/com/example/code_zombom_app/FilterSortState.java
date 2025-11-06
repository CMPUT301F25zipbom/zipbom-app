package com.example.code_zombom_app;

import java.io.Serializable;
import java.util.Date;

/**
 * Holds the entrant's current filter selections (interest and availability).
 * Date setters/getters clone values to protect internal state.
 */
public class FilterSortState implements Serializable {

    private boolean filterByInterests;
    private String selectedInterestCategory;
    private boolean filterByAvailability;
    private Date availabilityStart;
    private Date availabilityEnd;

    public FilterSortState() {
        filterByInterests = false;
        selectedInterestCategory = null;
        filterByAvailability = false;
        availabilityStart = null;
        availabilityEnd = null;
    }

    public static FilterSortState copyOf(FilterSortState other) {
        FilterSortState state = new FilterSortState();
        if (other == null) {
            return state;
        }
        state.setFilterByInterests(other.isFilterByInterests());
        state.setSelectedInterestCategory(other.getSelectedInterestCategory());
        state.setFilterByAvailability(other.isFilterByAvailability());
        state.setAvailabilityStart(other.getAvailabilityStart());
        state.setAvailabilityEnd(other.getAvailabilityEnd());
        return state;
    }

    public boolean isFilterByInterests() {
        return filterByInterests;
    }

    public void setFilterByInterests(boolean filterByInterests) {
        this.filterByInterests = filterByInterests;
        if (!filterByInterests) {
            selectedInterestCategory = null;
        }
    }

    public String getSelectedInterestCategory() {
        return selectedInterestCategory;
    }

    public void setSelectedInterestCategory(String selectedInterestCategory) {
        this.selectedInterestCategory = selectedInterestCategory;
    }

    public boolean isFilterByAvailability() {
        return filterByAvailability;
    }

    public void setFilterByAvailability(boolean filterByAvailability) {
        this.filterByAvailability = filterByAvailability;
        if (!filterByAvailability) {
            availabilityStart = null;
            availabilityEnd = null;
        }
    }

    public Date getAvailabilityStart() {
        return availabilityStart == null ? null : new Date(availabilityStart.getTime());
    }

    public void setAvailabilityStart(Date availabilityStart) {
        if (availabilityStart == null) {
            this.availabilityStart = null;
        } else {
            this.availabilityStart = new Date(availabilityStart.getTime());
        }
    }

    public Date getAvailabilityEnd() {
        return availabilityEnd == null ? null : new Date(availabilityEnd.getTime());
    }

    public void setAvailabilityEnd(Date availabilityEnd) {
        if (availabilityEnd == null) {
            this.availabilityEnd = null;
        } else {
            this.availabilityEnd = new Date(availabilityEnd.getTime());
        }
    }
}
