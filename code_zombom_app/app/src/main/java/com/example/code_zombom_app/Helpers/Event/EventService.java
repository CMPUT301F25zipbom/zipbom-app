package com.example.code_zombom_app.Helpers.Event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.code_zombom_app.organizer.EventForOrg;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.DocumentReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralised entry point for persisting and fetching events.
 * All role-specific screens should funnel writes through this service to avoid schema drift.
 */
public class EventService {

    private final FirebaseFirestore firestore;

    public EventService() {
        this(FirebaseFirestore.getInstance());
    }

    public EventService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Adds an entrant email to the waiting list transactionally.
     *
     * @param documentId Firestore document id for the event
     * @param entrantEmail entrant email to add
     * @return Task representing completion (success/failure will surface to listeners)
     */
    public Task<Void> addEntrantToWaitlist(@NonNull String documentId, @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            EventForOrg dto = transaction.get(eventRef).toObject(EventForOrg.class);
            if (dto == null) {
                throw new IllegalStateException("Event not found");
            }
            Event event = EventMapper.toDomain(dto, documentId);
            if (event == null) {
                throw new IllegalStateException("Invalid event data");
            }
            if (event.getChosenList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already been selected for this event.");
            }
            if (event.getWaitingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already joined this waiting list.");
            }
            int waitlistMaximum = event.getWaitlistLimit();
            if (waitlistMaximum <= 0) {
                waitlistMaximum = event.getCapacity();
            }
            if (waitlistMaximum > 0 && event.getWaitingList().size() >= waitlistMaximum) {
                throw new IllegalArgumentException("This waiting list is full.");
            }
            event.joinWaitingList(normalizedEmail);
            EventForOrg updatedDto = EventMapper.toDto(event);
            transaction.set(eventRef, updatedDto);
            return null;
        });
    }

    /**
     * Removes an entrant email from the waiting list transactionally.
     */
    public Task<Void> removeEntrantFromWaitlist(@NonNull String documentId, @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            EventForOrg dto = transaction.get(eventRef).toObject(EventForOrg.class);
            if (dto == null) {
                throw new IllegalStateException("Event not found");
            }
            Event event = EventMapper.toDomain(dto, documentId);
            if (event == null) {
                throw new IllegalStateException("Invalid event data");
            }
            if (!event.getWaitingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You are not on this waiting list.");
            }
            event.leaveWaitingList(normalizedEmail);
            EventForOrg updatedDto = EventMapper.toDto(event);
            transaction.set(eventRef, updatedDto);
            return null;
        });
    }

    /**
     * Creates or updates the backing Firestore document for the supplied event.
     * If {@link Event#getFirestoreDocumentId()} is empty a new document will be created.
     *
     * @param event canonical domain event to persist
     * @return Task representing the asynchronous save
     */
    public Task<Void> saveEvent(@NonNull Event event) {
        EventForOrg dto = EventMapper.toDto(event);

        String documentId = event.getFirestoreDocumentId();
        if (documentId == null || documentId.trim().isEmpty()) {
            documentId = firestore.collection("Events").document().getId();
            event.setFirestoreDocumentId(documentId);
            dto.setEventId(documentId);
        }

        return firestore.collection("Events")
                .document(documentId)
                .set(dto);
    }

    /**
     * Builds a canonical domain event from the organiser DTO.
     *
     * @param dto organiser model (typically from Firestore)
     * @param firestoreId document id to attach to the domain event
     * @return mapped domain event or null when the DTO is incomplete
     */
    @Nullable
    public Event mapToDomain(@Nullable EventForOrg dto, @Nullable String firestoreId) {
        return EventMapper.toDomain(dto, firestoreId);
    }

    /**
     * Runs a simple lottery: randomly choose up to the remaining capacity from the waiting list.
     * Selected entrants are moved to the chosen list and removed from the waiting list.
     *
     * @param documentId Firestore document id for the event
     */
    public Task<Void> runLotteryDraw(@NonNull String documentId) {
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            EventForOrg dto = transaction.get(eventRef).toObject(EventForOrg.class);
            if (dto == null) {
                throw new IllegalStateException("Event not found");
            }
            Event event = EventMapper.toDomain(dto, documentId);
            if (event == null) {
                throw new IllegalStateException("Invalid event data");
            }

            int capacity = Math.max(0, event.getCapacity());
            int alreadyFilled = event.getRegisteredList().size() + event.getChosenList().size();
            int slotsRemaining = capacity > 0 ? Math.max(0, capacity - alreadyFilled) : event.getWaitingList().size();
            if (slotsRemaining == 0) {
                return null; // nothing to do
            }

            List<String> candidates = new ArrayList<>(event.getWaitingList());
            candidates.removeAll(event.getChosenList());
            candidates.removeAll(event.getRegisteredList());
            Collections.shuffle(candidates);

            int picks = Math.min(slotsRemaining, candidates.size());
            List<String> winners = new ArrayList<>();
            for (int i = 0; i < picks; i++) {
                String winner = candidates.get(i);
                event.addChosenEntrant(winner);
                event.leaveWaitingList(winner);
                winners.add(winner);
            }
            
            // Mark the draw as complete
            event.setDrawComplete(true);
            event.setDrawTimestamp(System.currentTimeMillis()); // current time as draw timestamp


            //identify losers
            Set<String> losers = new HashSet<>(event.getWaitingList());
            losers.removeAll(event.getChosenList());
            losers.removeAll(event.getRegisteredList());

            EventForOrg updatedDto = EventMapper.toDto(event);
            transaction.set(eventRef, updatedDto);

            // Write winner/loser notifications under the event for entrant listeners
            for (String winner : winners) {
                transaction.set(eventRef.collection("Notifications").document(),
                        buildNotification(winner, "win", event.getName(), event.getDrawTimestamp()));
            }
            for (String loser : losers) {
                transaction.set(eventRef.collection("Notifications").document(),
                        buildNotification(loser, "lose", event.getName(), event.getDrawTimestamp()));
            }
            return null;
        });
    }

    /**
     * Deletes the specified event document.
     */
    public Task<Void> deleteEvent(@NonNull String documentId) {
        return firestore.collection("Events").document(documentId).delete();
    }

    private Map<String, Object> buildNotification(String recipientEmail, String type, String eventName, long drawTimestamp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientEmail", recipientEmail == null ? "" : recipientEmail.trim().toLowerCase());
        payload.put("type", type); // "win" or "lose"
        payload.put("eventName", eventName != null ? eventName : "");
        payload.put("drawTimestamp", drawTimestamp);
        payload.put("createdAt", System.currentTimeMillis());
        return payload;
    }

    /**
     * Records that a chosen entrant has accepted their invitation to participate.
     * Moves the entrant into the pending list so organisers can see who confirmed.
     */
    public Task<Void> acceptInvitation(@NonNull String documentId, @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            EventForOrg dto = transaction.get(eventRef).toObject(EventForOrg.class);
            if (dto == null) {
                throw new IllegalStateException("Event not found");
            }
            Event event = EventMapper.toDomain(dto, documentId);
            if (event == null) {
                throw new IllegalStateException("Invalid event data");
            }

            if (!event.getChosenList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You were not selected for this event.");
            }
            if (event.getPendingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already accepted this invitation.");
            }

            event.addPendingEntrant(normalizedEmail);
            EventForOrg updatedDto = EventMapper.toDto(event);
            transaction.set(eventRef, updatedDto);
            return null;
        });
    }

    /**
     * Records that a chosen entrant has declined their invitation; removes them from the winners list.
     * This keeps the Firestore state accurate so the organiser can re-run the draw if needed.
     */
    public Task<Void> declineInvitation(@NonNull String documentId, @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            EventForOrg dto = transaction.get(eventRef).toObject(EventForOrg.class);
            if (dto == null) {
                throw new IllegalStateException("Event not found");
            }
            Event event = EventMapper.toDomain(dto, documentId);
            if (event == null) {
                throw new IllegalStateException("Invalid event data");
            }

            if (!event.getChosenList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You were not selected for this event.");
            }

            event.removeChosenEntrant(normalizedEmail);
            event.removePendingEntrant(normalizedEmail);
            EventForOrg updatedDto = EventMapper.toDto(event);
            transaction.set(eventRef, updatedDto);
            return null;
        });
    }
}
