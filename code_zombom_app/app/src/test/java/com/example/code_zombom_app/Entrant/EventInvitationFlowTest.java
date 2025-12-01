package com.example.code_zombom_app.Entrant;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Invitation lifecycle tests for entrants accepting/declining and getting another chance (US 01.05.01–US 01.05.03).
 */
@RunWith(MockitoJUnitRunner.class)
public class EventInvitationFlowTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockEventsCollection;
    @Mock private DocumentReference mockEventDocumentRef;
    @Mock private DocumentSnapshot mockEventSnapshot;
    @Mock private Transaction mockTransaction;
    @Mock private CollectionReference mockResponsesCollection;
    @Mock private DocumentReference mockResponseDoc;

    private EventService eventService;

    private static final String EVENT_ID = "invitation-flow-event";
    private static final String EMAIL = "entrant@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
        when(mockEventDocumentRef.collection("Responses")).thenReturn(mockResponsesCollection);
        when(mockResponsesCollection.document(any())).thenReturn(mockResponseDoc);
    }

    private void prepareTransaction(Event event) throws FirebaseFirestoreException {
        when(mockTransaction.get(eq(mockEventDocumentRef))).thenReturn(mockEventSnapshot);
        when(mockEventSnapshot.toObject(Event.class)).thenReturn(event);
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

    private <T> T await(Task<T> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        throw new ExecutionException(task.getException());
    }

    @Test
    public void acceptInvitation_MovesChosenToPending() throws Exception {
        Event event = new Event("Accept Flow");
        event.setEventId(EVENT_ID);
        event.addChosenEntrant(EMAIL);

        prepareTransaction(event);

        Task<Void> task = eventService.acceptInvitation(EVENT_ID, EMAIL);
        await(task);

        assertTrue(event.getPendingList().contains(EMAIL));
        assertFalse(event.getChosenList().contains(EMAIL));
        verify(mockTransaction, atLeastOnce()).set(eq(mockResponseDoc), any(Map.class));
    }

    @Test
    public void declineInvitation_MovesEntrantToCancelled() throws Exception {
        Event event = new Event("Decline Flow");
        event.setEventId(EVENT_ID);
        event.addChosenEntrant(EMAIL);

        prepareTransaction(event);

        Task<Void> task = eventService.declineInvitation(EVENT_ID, EMAIL);
        await(task);

        assertFalse(event.getChosenList().contains(EMAIL));
        assertTrue(event.getCancelledList().contains(EMAIL));
    }

    @Test
    public void declineThenRunLottery_GivesAnotherEntrantChance() throws Exception {
        Event event = new Event("Replacement Flow");
        event.setEventId(EVENT_ID);
        event.setCapacity(1);
        event.joinWaitingList("first@example.com");
        event.joinWaitingList("second@example.com");

        prepareTransaction(event);

        // Initial draw picks one entrant.
        Task<Void> drawTask = eventService.runLotteryDraw(EVENT_ID);
        await(drawTask);
        assertEquals(1, event.getChosenList().size());

        String chosenEmail = event.getChosenList().get(0);
        Task<Void> declineTask = eventService.declineInvitation(EVENT_ID, chosenEmail);
        await(declineTask);
        assertFalse(event.getChosenList().contains(chosenEmail));

        Task<Void> secondDrawTask = eventService.runLotteryDraw(EVENT_ID);
        await(secondDrawTask);
        assertEquals("A replacement entrant should be promoted", 1, event.getChosenList().size());
    }
}


