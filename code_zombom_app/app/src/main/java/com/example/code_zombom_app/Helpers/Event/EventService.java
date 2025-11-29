package com.example.code_zombom_app.Helpers.Event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.code_zombom_app.Helpers.Mail.Mail;
import com.example.code_zombom_app.Helpers.Mail.MailService;
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
     * @param eventId The event's unique id
     * @param entrantEmail entrant email to add
     * @return Task representing completion (success/failure will surface to listeners)
     * @throws IllegalStateException If the event cannot be found in the database
     * @throws IllegalArgumentException If the entrant has already been selected or joined the
     *                                  waiting list or the maximum capacity for the waiting list
     *                                  has been reached
     */
    public Task<Void> addEntrantToWaitlist(@NonNull String eventId, @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(eventId);
            Event event = transaction.get(eventRef).toObject(Event.class);

            if (event == null) {
                throw new IllegalStateException("Event not found!");
            }
            if (event.getChosenList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already been selected for this event.");
            }
            if (event.getWaitingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already joined this waiting list.");
            }
            // Block entrants who already accepted (pending list) from rejoining the waitlist.
            if (event.getPendingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already accepted an invitation for this event.");
            }
            // If accepted entrants already meet capacity, block any further joins.
            int maxPeople = Math.max(0, event.getCapacity());
            if (maxPeople > 0 && event.getPendingList().size() >= maxPeople) {
                throw new IllegalArgumentException("This event is full.");
            }
            int waitlistMaximum = event.getWaitlistLimit();
            if (waitlistMaximum <= 0) {
                waitlistMaximum = event.getCapacity();
            }
            if (waitlistMaximum > 0 && event.getWaitingList().size() >= waitlistMaximum) {
                throw new IllegalArgumentException("This waiting list is full.");
            }
            event.joinWaitingList(normalizedEmail);
            transaction.set(eventRef, event);
            return null;
        });
    }

    /**
     * Removes an entrant email from the waiting list transactionally.
     */
    public Task<Void> removeEntrantFromWaitlist(@NonNull String eventId, @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(eventId);
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new IllegalStateException("Event not found");
            }

            if (!event.getWaitingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You are not on this waiting list.");
            }
            event.leaveWaitingList(normalizedEmail);
            transaction.set(eventRef, event);
            return null;
        });
    }

    /**
     * Creates or updates the backing Firestore document for the supplied event.
     *
     * @param event canonical domain event to persist
     * @return Task representing the asynchronous save
     */
    public Task<Void> saveEvent(@NonNull com.example.code_zombom_app.Helpers.Event.Event event) {
        String documentId = event.getEventId();

        return firestore.collection("Events")
                .document(documentId)
                .set(event);
    }

    /**
     * Runs a simple lottery: randomly choose up to the remaining capacity from the waiting list.
     * Selected entrants are moved to the chosen list and removed from the waiting list.
     *
     * @param documentId The event's document id
     */
    public Task<Void> runLotteryDraw(@NonNull String documentId) {
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new IllegalStateException("Event not found");
            }

            int capacity = Math.max(0, event.getCapacity());
            int acceptedCount = event.getPendingList().size();
            // Prevent drawing when accepted entrants have already filled or exceeded capacity.
            if (capacity > 0 && acceptedCount >= capacity) {
                return null;
            }
            // Treat accepted entrants as occupying seats when computing remaining capacity.
            int alreadyFilled = event.getRegisteredList().size() + event.getChosenList().size() + acceptedCount;
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

            transaction.set(eventRef, event);

            // Write winner/loser notifications under the event for entrant listeners
            for (String winner : winners) {
                transaction.set(eventRef.collection("Notifications").document(),
                        buildNotification(winner, "win", event.getName(), event.getDrawTimestamp()));

                Mail mail = new Mail(event.getName(), winner, Mail.MailType.INVITE_LOTTERY_WINNER);
                mail.setHeader("Invitation to register for event: " + event.getName());
                mail.setContent("Congratulation " + winner + "! You have been selected! To accept " +
                        "the invitation, pressed Accept. To decline, press Decline");
                MailService.sendMail(mail);
            }

            for (String loser : losers) {
                transaction.set(eventRef.collection("Notifications").document(),
                        buildNotification(loser, "lose", event.getName(), event.getDrawTimestamp()));

                Mail mail = new Mail(event.getName(), loser, Mail.MailType.DECLINE_LOTTERY_LOSER);
                mail.setHeader("Better luck next time");
                mail.setContent("We regret to inform that you have not been selected for the event: "
                + event.getName() + "!");
                MailService.sendMail(mail);
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
     * Builds a QR payload string from the canonical event state, falling back to poster URL when available.
     * //TODO: Maybe in the future build a QR code that when scanned take the entrant to the event in the app ONLY
     */
    public static String buildQrPayload(com.example.code_zombom_app.Helpers.Event.Event event, @Nullable String posterUrl) {
        StringBuilder qrDataBuilder = new StringBuilder();
        qrDataBuilder.append("Event: ").append(event != null ? nullToEmpty(event.getName()) : "").append("\n");
        qrDataBuilder.append("Location: ").append(event != null ? nullToEmpty(event.getLocation().toString()) : "").append("\n");
        qrDataBuilder.append("Date: ").append(event != null ? nullToEmpty(event.getEventStartDate().toString()) : "").append("\n");
        qrDataBuilder.append("Deadline: ").append(event != null ? nullToEmpty(event.getEventEndDate().toString()) : "").append("\n");
        qrDataBuilder.append("Description: ").append(event != null ? nullToEmpty(event.getDescription()) : "").append("\n");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            qrDataBuilder.append("Poster: ").append(posterUrl);
        }
        return qrDataBuilder.toString();
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
            event.removeChosenEntrant(normalizedEmail);
            EventForOrg updatedDto = EventMapper.toDto(event);
            // Ensure the entrant is no longer marked as cancelled if they accept later
            ArrayList<String> cancelled = dto.getCancelled_Entrants() != null
                    ? new ArrayList<>(dto.getCancelled_Entrants())
                    : new ArrayList<>();
            cancelled.remove(normalizedEmail);
            updatedDto.setCancelled_Entrants(cancelled);
            transaction.set(eventRef, updatedDto);
            // Persist the entrant's response so the UI can restore state after navigation/restart.
            transaction.set(eventRef.collection("Responses").document(normalizedEmail),
                    buildResponsePayload(normalizedEmail, "accepted",
                            "You have accepted the invitation" + formatEventSuffix(event.getName())));
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
            ArrayList<String> cancelled = dto.getCancelled_Entrants() != null
                    ? new ArrayList<>(dto.getCancelled_Entrants())
                    : new ArrayList<>();
            if (!cancelled.contains(normalizedEmail)) {
                cancelled.add(normalizedEmail);
            }
            updatedDto.setCancelled_Entrants(cancelled);
            transaction.set(eventRef, updatedDto);
            // Persist the entrant's response so the UI can restore state after navigation/restart.
            transaction.set(eventRef.collection("Responses").document(normalizedEmail),
                    buildResponsePayload(normalizedEmail, "declined",
                            "You have declined the invitation" + formatEventSuffix(event.getName())));
            return null;
        });
    }

    private Map<String, Object> buildResponsePayload(String email, String status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email == null ? "" : email.trim());
        payload.put("status", status);
        payload.put("message", message);
        payload.put("updatedAt", System.currentTimeMillis());
        return payload;
    }

    private String formatEventSuffix(@Nullable String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            return ".";
        }
        return " to " + eventName + ".";
    }
  private static String nullToEmpty( @Nullable String value) {return value == null? "" : value;
                                                             }
}
