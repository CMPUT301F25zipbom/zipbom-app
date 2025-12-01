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
public class EventServiceLeaveWaitlistTest {

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

    private static final String EVENT_ID = "event-123";
    private static final String EMAIL = "test@example.com";
    private static final String NORM = "test@example.com";

    // ----- helpers -----

    private void mockTransactionGet(DocumentSnapshot snapshot) throws FirebaseFirestoreException {
        // Transaction.get(...) declares "throws FirebaseFirestoreException"
        when(mockTransaction.get(any(DocumentReference.class))).thenReturn(snapshot);
    }

    /**
     * runTransaction stub that executes the lambda and:
     *  - returns a successful Task on success
     *  - returns a failed Task (Tasks.forException) if the lambda throws
     */
    private void mockSuccessfulTransaction() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Transaction.Function<Void> f = invocation.getArgument(0);
            try {
                f.apply(mockTransaction);
                return Tasks.forResult(null);
            } catch (Exception e) {
                return Tasks.forException(e);
            }
        }).when(mockFirestore).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Lightweight replacement for Tasks.await(...) that does NOT touch Android Looper.
     * Assumes the Task is already completed (true for all our stubs).
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

    private Event event() {
        Event e = new Event("Test");
        e.setEventId(EVENT_ID);
        e.setCapacity(10);
        e.setWaitlistLimit(20);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date(System.currentTimeMillis() + 86400000));
        return e;
    }

    @Before
    public void setup() throws FirebaseFirestoreException {
        MockitoAnnotations.initMocks(this);

        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(anyString())).thenReturn(mockProfileDocumentRef);
        when(mockProfileDocumentRef.collection("History")).thenReturn(mockHistoryCollection);
        when(mockHistoryCollection.document()).thenReturn(mockHistoryDocumentRef);
    }


    // ---------------- tests ----------------

    @Test
    public void successfulLeave() throws Exception {
        Event e = event();
        e.joinWaitingList(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        int before = e.getNumberOfWaiting();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        verify(mockTransaction).set(eq(mockEventDocumentRef), any(Event.class));
        assertFalse(e.getWaitingList().contains(NORM));
        assertEquals(before - 1, e.getNumberOfWaiting());
    }

    @Test
    public void notOnWaitlist() throws Exception {
        Event e = event();

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail();
        } catch (ExecutionException ex) {
            assertEquals("You are not on this waiting list.", ex.getCause().getMessage());
        }

        verify(mockTransaction, never()).set(eq(mockEventDocumentRef), any(Event.class));
    }

    @Test
    public void countersUpdateCorrectly() throws Exception {
        Event e = event();
        e.joinWaitingList("a");
        e.joinWaitingList("b");
        e.joinWaitingList(NORM);
        e.joinWaitingList("c");
        int before = e.getNumberOfWaiting();

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        assertEquals(before - 1, e.getNumberOfWaiting());
        assertFalse(e.getWaitingList().contains(NORM));
        assertTrue(e.getWaitingList().contains("a"));
        assertTrue(e.getWaitingList().contains("b"));
        assertTrue(e.getWaitingList().contains("c"));
    }

    @Test
    public void firestoreFailure() throws Exception {
        Event e = event();
        e.joinWaitingList(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);

        Exception ex = new Exception("Firestore transaction failed");
        doAnswer(inv -> Tasks.forException(ex))
                .when(mockFirestore).runTransaction(any(Transaction.Function.class));

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail();
        } catch (ExecutionException err) {
            assertEquals("Firestore transaction failed", err.getCause().getMessage());
        }

        // Since transaction failed, the in-memory event should be unchanged
        assertTrue(e.getWaitingList().contains(NORM));
    }

    @Test
    public void eventNotFound() throws Exception {
        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(null);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail();
        } catch (ExecutionException ex) {
            assertEquals("Event not found", ex.getCause().getMessage());
        }

        verify(mockTransaction, never()).set(eq(mockEventDocumentRef), any(Event.class));
    }

    @Test
    public void emailNormalization() throws Exception {
        Event e = event();
        e.joinWaitingList(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        String messy = "   test@example.com   ";
        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, messy);
        awaitTask(task);

        assertFalse(e.getWaitingList().contains(NORM));
    }

    @Test
    public void removesOnlySpecificEntrant() throws Exception {
        Event e = event();
        e.joinWaitingList("x");
        e.joinWaitingList(NORM);
        e.joinWaitingList("y");
        e.joinWaitingList("z");

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        assertFalse(e.getWaitingList().contains(NORM));
        assertTrue(e.getWaitingList().contains("x"));
        assertTrue(e.getWaitingList().contains("y"));
        assertTrue(e.getWaitingList().contains("z"));
    }
}
