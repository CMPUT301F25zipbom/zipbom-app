package com.example.code_zombom_app.Helpers.Event;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Centralised entry point for persisting and fetching events.
 * All role-specific screens should funnel writes through this service to avoid schema drift.
 * @author Deng Ngut
 * @version 11/24/2025
 */
public class EventService {

    private final FirebaseFirestore firestore;

    /**
     * Constructs the service using the default Firestore instance. Intended for production use.
     */
    public EventService() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructs the service with an explicit Firestore dependency (useful for testing).
     *
     * @param firestore Firestore instance backing all event reads/writes
     */
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
            recordHistory(transaction, event, normalizedEmail, Entrant.Status.WAITLISTED);
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
            recordHistory(transaction, event, normalizedEmail, Entrant.Status.LEAVE);
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
                recordHistory(transaction, event, winner, Entrant.Status.SELECTED);
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
                if (isNotificationsEnabled(transaction, winner)) {
                    transaction.set(eventRef.collection("Notifications").document(),
                            buildNotification(
                                    winner,
                                    "win",
                                    event.getName(),
                                    event.getDrawTimestamp(),
                                    event.getEventId(),
                                    "Congratulations! You are a lottery winner and have been selected for " + event.getName()
                            ));
                }
            }

            for (String loser : losers) {
                recordHistory(transaction, event, loser, Entrant.Status.NOT_SELECTED);
                if (isNotificationsEnabled(transaction, loser)) {
                    transaction.set(eventRef.collection("Notifications").document(),
                            buildNotification(
                                    loser,
                                    "lose",
                                    event.getName(),
                                    event.getDrawTimestamp(),
                                    event.getEventId(),
                                    "Sorry! You were not selected. You coudd still get a chance if a selected entrant declines. " + event.getName()
                            ));
                }
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
    // constructs the payload with fields for notification
    /**
     * Builds a Firestore document payload for a notification entry targeted to a specific entrant.
     */
    private Map<String, Object> buildNotification(String recipientEmail,
                                                  String type,
                                                  String eventName,
                                                  long drawTimestamp,
                                                  String eventId,
                                                  @Nullable String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientEmail", recipientEmail == null ? "" : recipientEmail.trim().toLowerCase());
        payload.put("type", type); // "win" or "lose"
        payload.put("eventName", eventName != null ? eventName : "");
        payload.put("eventId", eventId != null ? eventId : "");
        payload.put("message", message != null ? message : "");
        payload.put("seen", false);
        payload.put("drawTimestamp", drawTimestamp);
        payload.put("createdAt", System.currentTimeMillis());
        return payload;
    }

    /**
     * Builds a QR payload string from the canonical event state, falling back to poster URL when available.
     */
    public static String buildQrPayload(com.example.code_zombom_app.Helpers.Event.Event event, @Nullable String posterUrl) {
        StringBuilder qrDataBuilder = new StringBuilder();
        qrDataBuilder.append("Event: ").append(event != null ? nullToEmpty(event.getName()) : "").append("\n");
        String locationText = (event != null && event.getLocation() != null)
                ? event.getLocation().toString()
                : "";
        qrDataBuilder.append("Location: ").append(locationText).append("\n");
        qrDataBuilder.append("Date: ").append(event != null && event.getEventStartDate() != null
                ? event.getEventStartDate().toString() : "").append("\n");
        qrDataBuilder.append("Deadline: ").append(event != null && event.getEventEndDate() != null
                ? event.getEventEndDate().toString() : "").append("\n");
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
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new IllegalStateException("Event not found");
            }

            if (!event.getChosenList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You were not selected for this event.");
            }
            if (event.getPendingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already accepted this invitation.");
            }

            event.addPendingEntrant(normalizedEmail);
            event.removeChosenEntrant(normalizedEmail);
            recordHistory(transaction, event, normalizedEmail, Entrant.Status.CONFIRMED);
            ArrayList<String> cancelled = event.getCancelledList();
            cancelled.remove(normalizedEmail);
            event.setCancelledList(cancelled);
            transaction.set(eventRef, event);
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
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new IllegalStateException("Event not found");
            }

            if (!event.getChosenList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You were not selected for this event.");
            }

            event.removeChosenEntrant(normalizedEmail);
            event.removePendingEntrant(normalizedEmail);
            recordHistory(transaction, event, normalizedEmail, Entrant.Status.DECLINED);
            ArrayList<String> cancelled = event.getCancelledList();
            if (!cancelled.contains(normalizedEmail)) {
                cancelled.add(normalizedEmail);
            }
            event.setCancelledList(cancelled);
            transaction.set(eventRef, event);
            // Persist the entrant's response so the UI can restore state after navigation/restart.
            transaction.set(eventRef.collection("Responses").document(normalizedEmail),
                    buildResponsePayload(normalizedEmail, "declined",
                            "You have declined the invitation" + formatEventSuffix(event.getName())));
            return null;
        });
    }

    /**
     * Converts a pending entrant into a fully registered participant and emits a confirmation
     * notification that honours the entrant's notification preference.
     */
    public Task<Void> completeRegistration(@NonNull String documentId,
                                           @NonNull String entrantEmail) {
        final String normalizedEmail = entrantEmail.trim();
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new IllegalStateException("Event not found");
            }
            if (!event.getPendingList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("Please accept the invitation before registering.");
            }
            if (event.getRegisteredList().contains(normalizedEmail)) {
                throw new IllegalArgumentException("You have already registered for this event.");
            }

            event.removePendingEntrant(normalizedEmail);
            event.addRegisteredEntrant(normalizedEmail);
            recordHistory(transaction, event, normalizedEmail, Entrant.Status.REGISTERED);

            transaction.set(eventRef, event);

            String successMessage = registrationSuccessMessage(event.getName());
            // Update the response record so UIs can detect that registration is complete.
            transaction.set(eventRef.collection("Responses").document(normalizedEmail),
                    buildResponsePayload(normalizedEmail, "registered", successMessage));

            if (isNotificationsEnabled(transaction, normalizedEmail)) {
                transaction.set(eventRef.collection("Notifications").document(),
                        buildNotification(
                                normalizedEmail,
                                "signup_success",
                                event.getName(),
                                System.currentTimeMillis(),
                                event.getEventId(),
                                successMessage
                        ));
            }
            return null;
        });
    }

    /**
     * Persists the acceptance/registration response from an entrant so the UI can resume state.
     */
    private Map<String, Object> buildResponsePayload(String email, String status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email == null ? "" : email.trim());
        payload.put("status", status);
        payload.put("message", message);
        payload.put("updatedAt", System.currentTimeMillis());
        return payload;
    }

    /**
     * Formats an event name for use in user-facing response messages.
     */
    private String formatEventSuffix(@Nullable String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            return ".";
        }
        return " to " + eventName + ".";
    }

    /**
     * Writes/updates a history record for an entrant inside the current transaction so that
     * their timeline reflects the latest interaction with this event (waitlisted, selected,
     * confirmed, declined, registered). Also mirrors the status into the profile's eventHistory map
     * for quick lookup without an additional read.
     *
     * @param transaction active Firestore transaction
     * @param event       canonical event state involved in the update
     * @param entrantEmail entrant identifier (email)
     * @param status      latest status to record
     */
    private void recordHistory(@NonNull Transaction transaction,
                               @Nullable Event event,
                               @NonNull String entrantEmail,
                               @NonNull Entrant.Status status) {
        if (event == null) {
            return;
        }
        String normalizedEmail = entrantEmail.trim();
        DocumentReference historyRef = firestore.collection("Profiles")
                .document(normalizedEmail)
                .collection("History")
                .document(); // generate unique entry per status change

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.getEventId());
        payload.put("eventName", event.getName() == null ? "" : event.getName());
        payload.put("status", status.name());
        payload.put("updatedAt", new Date());
        payload.put("startDate", event.getEventStartDate());
        payload.put("endDate", event.getEventEndDate());
        payload.put("location", event.getLocation() != null ? event.getLocation().toString() : "");

        transaction.set(historyRef, payload);

        // Also reflect the latest status in the profile map for quick lookups.
        DocumentReference profileRef = firestore.collection("Profiles").document(normalizedEmail);
        Map<String, Object> historyMapUpdate = new HashMap<>();
        historyMapUpdate.put("eventHistory." + event.getEventId(), status.name());
        transaction.set(profileRef, historyMapUpdate, SetOptions.merge());
    }

    /**
     * Checks if the entrant has notifications enabled. Defaults to true when the profile
     * is missing the flag or the profile document does not exist.
     * Determines whether organizer/admin notifications should be sent to the given entrant.
     */
    private boolean isNotificationsEnabled(@NonNull Transaction transaction, @NonNull String email) {
        String normalized = normalizeEmailKey(email);
        if (normalized.isEmpty()) {
            return false;
        }

        DocumentReference preferenceRef = firestore.collection("NotificationPreferences")
                .document(normalized);
        try {
            DocumentSnapshot snapshot = transaction.get(preferenceRef);
            if (snapshot.exists()) {
                Boolean enabled = snapshot.getBoolean("notificationEnabled");
                if (enabled != null) {
                    return enabled;
                }
            }
        } catch (Exception ignored) {
            // fall back to profile lookups
        }

        List<String> profileKeys = Arrays.asList(email.trim(), normalized);
        for (String key : profileKeys) {
            if (key.isEmpty()) {
                continue;
            }
            DocumentReference profileRef = firestore.collection("Profiles").document(key);
            try {
                DocumentSnapshot snapshot = transaction.get(profileRef);
                if (!snapshot.exists()) {
                    continue;
                }
                Boolean enabled = snapshot.getBoolean("notificationEnabled");
                if (enabled != null) {
                    return enabled;
                }
            } catch (Exception ignored) {
                // try next
            }
        }

        return true;
    }

    /**
     * Normalizes an email address for collection/document keys.
     */
    private String normalizeEmailKey(@Nullable String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public Task<Void> notifyWaitlistEntrants(@NonNull String eventId, @Nullable String message) {
        return notifyGroup(eventId, NotificationGroup.WAITLIST, "org_waitlist", message);
    }

    public Task<Void> notifySelectedEntrants(@NonNull String eventId, @Nullable String message) {
        return notifyGroup(eventId, NotificationGroup.SELECTED, "org_selected", message);
    }

    public Task<Void> notifyCancelledEntrants(@NonNull String eventId, @Nullable String message) {
        return notifyGroup(eventId, NotificationGroup.CANCELLED, "org_cancelled", message);
    }

    /**
     * Moves all entrants from the pending (accepted invite) list to the cancelled list.
     * This is intended for use after a lottery draw and acceptance period have concluded.
     *
     * @param documentId The event's document id
     * @return Task representing completion of the transaction.
     */
    public Task<Void> cancelUnregisteredEntrants(@NonNull String documentId) {
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(documentId);
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                // This will trigger the error you saw before if the object can't be created
                throw new IllegalStateException("Error converting document to Event object. Check Firestore field names!");
            }

            // Get the current lists from the event object
            ArrayList<String> chosenList = event.getChosenList();
            ArrayList<String> cancelledList = event.getCancelledList();

            // Nothing to do if the pending list is already empty
            if (chosenList == null || chosenList.isEmpty()) {
                return null;
            }
            // Iterate through a copy of the pending list to avoid modification issues during the loop
            for (String entrantEmail : new ArrayList<>(chosenList)) {
                // Add the entrant to the cancelled list if they aren't already there
                if (!cancelledList.contains(entrantEmail)) {
                    cancelledList.add(entrantEmail);
                }
                // Record this specific action in the entrant's history
                recordHistory(transaction, event, entrantEmail, Entrant.Status.CANCELLED);
            }

            // Clear the original pending list completely
            chosenList.clear();

            // Set the modified lists back to the event object to ensure they are saved
            event.setChosenList(chosenList);
            event.setCancelledList(cancelledList);

            // Persist all the changes to the event document in Firestore.
            transaction.set(eventRef, event);
            return null;
        });
    }

    private enum NotificationGroup { WAITLIST, SELECTED, CANCELLED }

    /**
     * Writes organizer-triggered notifications to the chosen entrant group within an event.
     */
    private Task<Void> notifyGroup(@NonNull String eventId,
                                   @NonNull NotificationGroup group,
                                   @NonNull String type,
                                   @Nullable String message) {
        return firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference eventRef = firestore.collection("Events").document(eventId);
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new IllegalStateException("Event not found");
            }

            List<String> recipients = new ArrayList<>();
            switch (group) {
                case WAITLIST:
                    recipients.addAll(event.getWaitingList());
                    break;
                case SELECTED:
                    recipients.addAll(event.getChosenList());
                    recipients.addAll(event.getPendingList());
                    break;
                case CANCELLED:
                    recipients.addAll(event.getCancelledList());
                    break;
            }

            String finalMessage = (message != null && !message.trim().isEmpty())
                    ? message
                    : defaultMessageForType(type, event.getName());

            for (String recipient : recipients) {
                if (!isNotificationsEnabled(transaction, recipient)) {
                    continue;
                }
                transaction.set(eventRef.collection("Notifications").document(),
                        buildNotification(
                                recipient,
                                type,
                                event.getName(),
                                System.currentTimeMillis(),
                                event.getEventId(),
                                finalMessage
                        ));
            }
            return null;
        });
    }

    /**
     * Provides a default notification message when the organizer does not supply one explicitly.
     */
    private String defaultMessageForType(String type, @Nullable String eventName) {
        String name = (eventName == null || eventName.trim().isEmpty()) ? "this event" : eventName;
        switch (type) {
            case "org_waitlist":
                return "You have been added to the waiting list for " + name;
            case "org_selected":
                return "Congratulations! You are a lottery winner and have been selected for " + name;
            case "org_cancelled":
                return "Your selection has been cancelled for " + name;
            case "win":
                return "Congratulations! You are a lottery winner and have been selected for " + name;
            case "lose":
                return "You were not selected this time for " + name + "but you can still get a chance if a selected entrant declines";
            case "signup_success":
                return registrationSuccessMessage(eventName);
            default:
                return "Update for " + name;
        }
    }

  private static String nullToEmpty( @Nullable String value) {return value == null? "" : value;
                                                               }

    /**
     * Formats the confirmation message sent after an entrant completes registration.
     */
    private String registrationSuccessMessage(@Nullable String eventName) {
        String name = (eventName == null || eventName.trim().isEmpty()) ? "this event" : eventName;
        return "You have successfully been registered to participate in " + name;
    }
}
