package com.example.code_zombom_app.Helpers.Event;

import androidx.annotation.Nullable;

import com.example.code_zombom_app.organizer.EventForOrg;

import java.util.ArrayList;

/**
 * Central mapping helpers between the organiser Firestore DTO and the shared domain Event model.
 * This keeps entrant/organiser/admin screens aligned on a single canonical representation.
 * @author Deng Ngut
 */
public final class EventMapper {

    private EventMapper() {}


    /**
     * Converts the domain event back into the organiser DTO shape for Firestore writes.
     */
    public static EventForOrg toDto(Event event) {
        EventForOrg dto = new EventForOrg();
        if (event == null) {
            return dto;
        }
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDate(event.getEventStartDate().toString());
        dto.setDeadline(event.getEventEndDate().toString());
        dto.setGenre(event.getGenre());
        if (event.getLocation() != null) {
            dto.setLocation(event.getLocation());
        }
        dto.setDescription(event.getDescription());
        dto.setMax_People(String.valueOf(event.getCapacity()));
        dto.setWait_List_Maximum(String.valueOf(event.getWaitlistLimit()));
        dto.setEntrants(event.getWaitingList());
        dto.setCancelled_Entrants(event.getCancelledList());
        dto.setAccepted_Entrants(event.getPendingList());
        dto.setLottery_Winners(event.getChosenList());
        dto.setPosterUrl(event.getPosterUrl());
        dto.setDrawComplete(event.isDrawComplete());
        dto.setDrawTimestamp(event.getDrawTimestamp());
        return dto;
    }

}
