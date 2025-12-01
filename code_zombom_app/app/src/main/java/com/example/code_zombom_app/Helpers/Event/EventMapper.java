package com.example.code_zombom_app.Helpers.Event;

import androidx.annotation.Nullable;

import com.example.code_zombom_app.organizer.EventForOrg;

import java.util.ArrayList;

/**
 * Central mapping helpers between the organiser Firestore DTO and the shared domain Event model.
 * This keeps entrant/organiser/admin screens aligned on a single canonical representation.
 */
public final class EventMapper {

    private EventMapper() {}

    /**
     * Converts an organiser-facing DTO into the domain {@link Event} while preserving the Firestore id.
     *
     * @param source organiser DTO loaded from Firestore
     * @param firestoreId backing document id
     * @return populated domain event, or null when mandatory fields are missing
     */
    @Nullable
    public static Event toDomain(@Nullable EventForOrg source, @Nullable String firestoreId) {
        if (source == null) {
            return null;
        }

        String name = source.getName();
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Event event = new Event(name);

        // Basic text fields
        //event.setLocation(source.getLocation());
        event.setMaxEntrants(Integer.parseInt(source.getMax_People()));
        //event.setEventDate(source.getDate());
        //event.setRegistrationClosesAt(source.getDeadline());
        event.setDescription(source.getDescription());
        event.setPosterUrl(source.getPosterUrl());
        if (source.getDrawComplete() != null) {
            event.setDrawComplete(source.getDrawComplete());
        }
        if (source.getDrawTimestamp() != null) {
            event.setDrawTimestamp(source.getDrawTimestamp());
        }

        // Capacity (defaults to zero when missing/malformed)
        try {
            event.setCapacity(Integer.parseInt(nullToEmpty(source.getMax_People())));
        } catch (NumberFormatException ignored) {
            event.setCapacity(0);
        }
        try {
            event.setWaitlistLimit(Integer.parseInt(nullToEmpty(source.getWait_List_Maximum())));
        } catch (NumberFormatException ignored) {
            event.setWaitlistLimit(0);
        }

        // Genre -> category (store unrecognised genres as restrictions for visibility)
        String genre = source.getGenre();
        event.setGenre(genre);

        // Lists
        for (String entrant : safeList(source.getEntrants())) {
            event.joinWaitingList(entrant);
        }
        for (String winner : safeList(source.getLottery_Winners())) {
            event.addChosenEntrant(winner);
        }
        for (String accepted : safeList(source.getAccepted_Entrants())) {
            event.addPendingEntrant(accepted);
        }

        // Store the best-known wait count when entrants were not hydrated
        event.setWaitingEntrantCount(event.getWaitingList().size());

        return event;
    }

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
        // No direct place for pending/cancelled in the domain yet.
        return dto;
    }

    /**
     * Builds a QR payload string from the canonical event state, falling back to poster URL when available.
     * @depreciate Each event now has a QR code upon creation
     */
    public static String buildQrPayload(Event event, @Nullable String posterUrl) {
        StringBuilder qrDataBuilder = new StringBuilder();
        qrDataBuilder.append("Event: ")
                .append(event != null ? nullToEmpty(event.getName()) : "")
                .append("\n");
        qrDataBuilder.append("Location: ").
                append(event != null ? nullToEmpty(event.getLocation().toString()) : "")
                .append("\n");
        qrDataBuilder.append("Date: ")
                .append(event != null ? nullToEmpty(event.getEventStartDate().toString()) : "")
                .append("\n");
        qrDataBuilder.append("Deadline: ")
                .append(event != null ? nullToEmpty(event.getEventEndDate().toString()) : "")
                .append("\n");
        qrDataBuilder.append("Description: ")
                .append(event != null ? nullToEmpty(event.getDescription()) : "").append("\n");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            qrDataBuilder.append("Poster: ").append(posterUrl);
        }
        return qrDataBuilder.toString();
    }

    private static ArrayList<String> safeList(@Nullable ArrayList<String> source) {
        return source != null ? source : new ArrayList<>();
    }

    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
