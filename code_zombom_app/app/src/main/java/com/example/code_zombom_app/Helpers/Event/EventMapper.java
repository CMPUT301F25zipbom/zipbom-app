//package com.example.code_zombom_app.Helpers.Event;
//
//import androidx.annotation.Nullable;
//
//import com.example.code_zombom_app.organizer.EventForOrg;
//
//import java.util.ArrayList;
//
///**
// * Central mapping helpers between the organiser Firestore DTO and the shared domain Event model.
// * This keeps entrant/organiser/admin screens aligned on a single canonical representation.
// */
//public final class EventMapper {
//
//    private EventMapper() {}
//
//    /**
//     * Converts an organiser-facing DTO into the domain {@link com.example.code_zombom_app.Helpers.Event.Event} while preserving the Firestore id.
//     *
//     * @param source organiser DTO loaded from Firestore
//     * @param firestoreId backing document id
//     * @return populated domain event, or null when mandatory fields are missing
//     */
//    @Nullable
//    public static com.example.code_zombom_app.Helpers.Event.Event toDomain(@Nullable EventForOrg source, @Nullable String firestoreId) {
//        if (source == null) {
//            return null;
//        }
//
//        String name = source.getName();
//        if (name == null || name.trim().isEmpty()) {
//            return null;
//        }
//
//        com.example.code_zombom_app.Helpers.Event.Event event = new com.example.code_zombom_app.Helpers.Event.Event(name);
//        if (firestoreId != null && !firestoreId.trim().isEmpty()) {
//            event.setFirestoreDocumentId(firestoreId);
//        }
//
//        // Basic text fields
//        event.setLocation(source.getLocation());
//        event.setMaxEntrants(Integer.parseInt(source.getMax_People()));
//        event.setEventDate(source.getDate());
//        event.setRegistrationClosesAt(source.getDeadline());
//        event.setDescription(source.getDescription());
//        event.setPosterUrl(source.getPosterUrl());
//        if (source.getDrawComplete() != null) {
//            event.setDrawComplete(source.getDrawComplete());
//        }
//        if (source.getDrawTimestamp() != null) {
//            event.setDrawTimestamp(source.getDrawTimestamp());
//        }
//
//        // Capacity (defaults to zero when missing/malformed)
//        try {
//            event.setCapacity(Integer.parseInt(nullToEmpty(source.getMax_People())));
//        } catch (NumberFormatException ignored) {
//            event.setCapacity(0);
//        }
//        try {
//            event.setWaitlistLimit(Integer.parseInt(nullToEmpty(source.getWait_List_Maximum())));
//        } catch (NumberFormatException ignored) {
//            event.setWaitlistLimit(0);
//        }
//
//        // Genre -> category (store unrecognised genres as restrictions for visibility)
//        String genre = source.getGenre();
//        if (genre != null && !genre.trim().isEmpty()) {
//            try {
//                event.addCategory(genre);
//            } catch (IllegalArgumentException ex) {
//                event.addRestriction("Category: " + genre);
//            }
//        }
//
//        // Lists
//        for (String entrant : safeList(source.getEntrants())) {
//            event.joinWaitingList(entrant);
//        }
//        for (String winner : safeList(source.getLotteryWinners())) {
//            event.addChosenEntrant(winner);
//        }
////        for (String accepted : safeList(source.getAccepted_Entrants())) {
////            event.addRegisteredEntrant(accepted);
////        }
//
//        // Store the best-known wait count when entrants were not hydrated
//        event.setWaitingEntrantCount(event.getWaitingList().size());
//
//        return event;
//    }
//
//    /**
//     * Converts the domain event back into the organiser DTO shape for Firestore writes.
//     */
//    public static EventForOrg toDto(com.example.code_zombom_app.Helpers.Event.Event event) {
//        EventForOrg dto = new EventForOrg();
//        if (event == null) {
//            return dto;
//        }
//        dto.setEventId(event.getFirestoreDocumentId());
//        dto.setName(event.getName());
//        dto.setDate(event.getEventDateText());
//        dto.setDeadline(event.getRegistrationClosesAtText());
//        dto.setGenre(firstCategory(event));
//        dto.setLocation(event.getLocation());
//        dto.setDescription(event.getDescription());
//        dto.setMax_People(String.valueOf(event.getCapacity()));
//        dto.setWait_List_Maximum(String.valueOf(event.getWaitlistLimit()));
//        dto.setEntrants(event.getWaitingList());
////        dto.setAccepted_Entrants(event.getRegisteredList());
//        dto.setLotteryWinners(event.getChosenList());
//        dto.setPosterUrl(event.getPosterUrl());
//        dto.setDrawComplete(event.isDrawComplete());
//        dto.setDrawTimestamp(event.getDrawTimestamp());
//        // No direct place for pending/cancelled in the domain yet.
//        return dto;
//    }
//
//    private static ArrayList<String> safeList(@Nullable ArrayList<String> source) {
//        return source != null ? source : new ArrayList<>();
//    }
//
//    private static String firstCategory(com.example.code_zombom_app.Helpers.Event.Event event) {
//        ArrayList<String> categories = event.getCategories();
//        if (categories.isEmpty()) {
//            return "";
//        }
//        return categories.get(0);
//    }
//}
