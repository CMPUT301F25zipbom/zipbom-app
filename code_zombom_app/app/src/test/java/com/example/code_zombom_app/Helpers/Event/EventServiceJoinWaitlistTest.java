package com.example.code_zombom_app.Helpers.Event;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceJoinWaitlistTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private com.google.firebase.firestore.CollectionReference mockEventsCollection;
    @Mock private com.google.firebase.firestore.CollectionReference mockProfilesCollection;
    @Mock private DocumentReference mockEventDocumentRef;
    @Mock private DocumentReference mockProfileDocumentRef;
    @Mock private com.google.firebase.firestore.CollectionReference mockHistoryCollection;
    @Mock private DocumentReference mockHistoryDocumentRef;
    @Mock private DocumentSnapshot mockEventDocumentSnapshot;
    @Mock private DocumentSnapshot mockProfileDocumentSnapshot;
    @Mock private Transaction mockTransaction;

    private EventService eventService;

    private static final String TEST_EVENT_ID = "event-123";
    private static final String EMAIL = "test@example.com";
    private static final String NORM_EMAIL = "test@example.com";

    // -----------------------
    // UNIVERSAL FIX HELPERS
    // -----------------------

    /** Fix for FirestoreException from transaction.get() */
    private void mockTransactionGet(DocumentSnapshot snapshot) throws FirebaseFirestoreException {
        // transaction.get(...) declares "throws FirebaseFirestoreException"
        when(mockTransaction.get(any(DocumentReference.class)))
                .thenReturn(snapshot);
    }

    /**
     * Fix for runTransaction needing to execute the lambda AND propagate errors via Task,
     * instead of throwing directly.
     */
    private void mockSuccessfulTransaction() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Transaction.Function<Void> function = invocation.getArgument(0);
            try {
                // Execute the transaction body (our EventService lambda)
                function.apply(mockTransaction);
                // Success path -> completed Task
                return Tasks.forResult(null);
            } catch (Exception e) {
                // Failure path -> Task with exception, so tests can assert via ExecutionException
                return Tasks.forException(e);
            }
        }).when(mockFirestore).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Lightweight replacement for Tasks.await(...) that does NOT touch Android Looper.
     * It assumes the Task is already complete (which is true for our stubs).
     */
    private <T> T awaitTask(Task<T> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        Exception e = task.getException();
        if (e != null) {
            throw new ExecutionException(e);
        }
        return null;
    }

    private Event createEvent() {
        Event e = new Event("Test Event");
        e.setEventId(TEST_EVENT_ID);
        e.setCapacity(10);
        e.setWaitlistLimit(20);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date(System.currentTimeMillis() + 86400000));
        return e;
    }

    @Before
    public void setUp() throws FirebaseFirestoreException {
        MockitoAnnotations.initMocks(this);

        // IMPORTANT: disable QR generation so Event() doesn't touch Bitmap APIs in JVM tests
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(TEST_EVENT_ID)).thenReturn(mockEventDocumentRef);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(anyString())).thenReturn(mockProfileDocumentRef);
        when(mockProfileDocumentRef.collection("History")).thenReturn(mockHistoryCollection);
        when(mockHistoryCollection.document()).thenReturn(mockHistoryDocumentRef);

        // This also calls transaction.get(...), so it must handle the checked exception
        when(mockTransaction.get(mockProfileDocumentRef)).thenReturn(mockProfileDocumentSnapshot);
        // Match the actual field your production code uses
        when(mockProfileDocumentSnapshot.getBoolean("notificationEnabled")).thenReturn(true);
    }

    // ----------------------------------------------------
    // TESTS FOR addEntrantToWaitlist()
    // ----------------------------------------------------

    @Test
    public void addEntrant_SuccessfulJoin() throws Exception {
        Event event = createEvent();

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, EMAIL);
        // Do NOT use Tasks.await (uses Looper); use our helper
        awaitTask(task);

        verify(mockTransaction).set(eq(mockEventDocumentRef), any(Event.class));
        assertTrue(event.getWaitingList().contains(NORM_EMAIL));
    }

    @Test
    public void addEntrant_AlreadyOnWaitlist() throws Exception {
        Event event = createEvent();
        event.joinWaitingList(NORM_EMAIL);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail("Expected exception");
        } catch (ExecutionException ex) {
            assertEquals("You have already joined this waiting list.", ex.getCause().getMessage());
        }

        verify(mockTransaction, never()).set(eq(mockEventDocumentRef), any(Event.class));
    }

    @Test
    public void addEntrant_WaitlistFull() throws Exception {
        Event event = createEvent();
        event.setWaitlistLimit(2);
        event.joinWaitingList("u1");
        event.joinWaitingList("u2");

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail("Expected exception");
        } catch (ExecutionException ex) {
            assertEquals("This waiting list is full.", ex.getCause().getMessage());
        }

        verify(mockTransaction, never()).set(eq(mockEventDocumentRef), any(Event.class));
    }

    @Test
    public void addEntrant_AlreadyInChosenList() throws Exception {
        Event event = createEvent();
        event.addChosenEntrant(NORM_EMAIL);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail("Expected exception");
        } catch (ExecutionException ex) {
            assertEquals("You have already been selected for this event.", ex.getCause().getMessage());
        }
    }

    @Test
    public void addEntrant_AlreadyInPendingList() throws Exception {
        Event event = createEvent();
        event.addPendingEntrant(NORM_EMAIL);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail("Expected exception");
        } catch (ExecutionException ex) {
            assertEquals("You have already accepted an invitation for this event.", ex.getCause().getMessage());
        }
    }

    @Test
    public void addEntrant_EventFull() throws Exception {
        Event event = createEvent();
        event.setCapacity(1);
        event.addPendingEntrant("already");

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail("Expected exception");
        } catch (ExecutionException ex) {
            assertEquals("This event is full.", ex.getCause().getMessage());
        }
    }

    @Test
    public void emailNormalization_TrimmedCorrectly() throws Exception {
        Event event = createEvent();

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(event);

        mockSuccessfulTransaction();

        String messy = "   test@example.com   ";
        Task<Void> task = eventService.addEntrantToWaitlist(TEST_EVENT_ID, messy);
        awaitTask(task);

        assertTrue(event.getWaitingList().contains(NORM_EMAIL));
        assertFalse(event.getWaitingList().contains(messy));
    }
}
