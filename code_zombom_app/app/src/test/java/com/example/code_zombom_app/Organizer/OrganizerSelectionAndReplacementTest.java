// File: app/src/test/java/com/example/code_zombom_app/organizer/OrganizerSelectionAndReplacementTest.java
package com.example.code_zombom_app.organizer;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Organizer-focused tests for lottery sampling, replacement, and cancellation flows
 * (US 02.05.02, 02.05.03, 02.06.01–02.06.04).
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizerSelectionAndReplacementTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private DocumentReference mockEventDocumentRef;

    @Mock
    private DocumentSnapshot mockEventSnapshot;

    @Mock
    private Transaction mockTransaction;

    private EventService eventService;

    private static final String EVENT_ID = "org-lottery-event";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
    }

    private void mockTransactionGet(Event event) throws Exception {
        when(mockTransaction.get(mockEventDocumentRef)).thenReturn(mockEventSnapshot);
        when(mockEventSnapshot.toObject(Event.class)).thenReturn(event);
    }

    private void mockSuccessfulTransaction() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Transaction.Function<Void> fn = invocation.getArgument(0);
            try {
                fn.apply(mockTransaction);
                return Tasks.forResult(null);
            } catch (Exception e) {
                return Tasks.forException(e);
            }
        }).when(mockFirestore).runTransaction(any(Transaction.Function.class));
    }

    private <T> T awaitTask(Task<T> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        throw new ExecutionException(task.getException());
    }

    @Test
    public void runLotteryDraw_SamplesUpToCapacityFromWaitingList() throws Exception {
        Event event = new Event("Lottery Capacity Test");
        event.setEventId(EVENT_ID);
        event.setCapacity(3);
        event.setWaitlistLimit(10);

        event.joinWaitingList("a@example.com");
        event.joinWaitingList("b@example.com");
        event.joinWaitingList("c@example.com");
        event.joinWaitingList("d@example.com");

        mockTransactionGet(event);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.runLotteryDraw(EVENT_ID);
        awaitTask(task);

        ArrayList<String> chosen = event.getChosenList();
        ArrayList<String> waiting = event.getWaitingList();

        assertTrue("Chosen size must not exceed capacity", chosen.size() <= 3);

        for (String email : chosen) {
            assertTrue("Chosen entrants must come from original waiting list",
                    email.equals("a@example.com") ||
                            email.equals("b@example.com") ||
                            email.equals("c@example.com") ||
                            email.equals("d@example.com"));
        }

        assertTrue("Waiting list size plus chosen should equal original count",
                chosen.size() + waiting.size() == 4);

        assertTrue("Draw should be marked complete", event.isDrawComplete());
    }

    @Test
    public void runLotteryDraw_NoSlotsRemaining_DoesNothing() throws Exception {
        Event event = new Event("Full Event");
        event.setEventId(EVENT_ID);
        event.setCapacity(2);

        event.addPendingEntrant("x@example.com");
        event.addPendingEntrant("y@example.com");

        event.joinWaitingList("w@example.com");
        event.joinWaitingList("z@example.com");

        mockTransactionGet(event);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.runLotteryDraw(EVENT_ID);
        awaitTask(task);

        assertEquals("Chosen list should remain empty when pending already fills capacity",
                0, event.getChosenList().size());
        assertEquals("Waiting list should remain unchanged",
                2, event.getWaitingList().size());
    }

    @Test
    public void cancelUnregisteredEntrants_MovesChosenToCancelledAndClearsChosen() throws Exception {
        Event event = new Event("Cancel Unregistered Test");
        event.setEventId(EVENT_ID);

        event.addChosenEntrant("winner1@example.com");
        event.addChosenEntrant("winner2@example.com");
        event.addCancelledEntrant("existing_cancel@example.com");

        mockTransactionGet(event);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.cancelUnregisteredEntrants(EVENT_ID);
        awaitTask(task);

        assertEquals("Chosen list should be cleared", 0, event.getChosenList().size());
        assertTrue("Cancelled list should contain former chosen entrants",
                event.getCancelledList().contains("winner1@example.com") &&
                        event.getCancelledList().contains("winner2@example.com"));
        assertTrue("Existing cancelled entrants should be preserved",
                event.getCancelledList().contains("existing_cancel@example.com"));

        verify(mockFirestore, atLeastOnce()).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void acceptInvitation_MovesChosenToPendingAndRecordsConfirmed() throws Exception {
        Event event = new Event("Accept Invite Test");
        event.setEventId(EVENT_ID);

        String email = "chosen@example.com";
        event.addChosenEntrant(email);

        mockTransactionGet(event);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.acceptInvitation(EVENT_ID, email);
        awaitTask(task);

        assertTrue("Pending list should contain accepted entrant",
                event.getPendingList().contains(email));
        assertTrue("Chosen list should no longer contain accepted entrant",
                !event.getChosenList().contains(email));
    }

    @Test
    public void declineInvitation_MovesChosenToCancelled() throws Exception {
        Event event = new Event("Decline Invite Test");
        event.setEventId(EVENT_ID);

        String email = "decline@example.com";
        event.addChosenEntrant(email);

        mockTransactionGet(event);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.declineInvitation(EVENT_ID, email);
        awaitTask(task);

        assertTrue("Chosen list should not contain declined entrant",
                !event.getChosenList().contains(email));
        assertTrue("Cancelled list should contain declined entrant",
                event.getCancelledList().contains(email));
    }
}
