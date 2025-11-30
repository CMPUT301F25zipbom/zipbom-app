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
public class EventServiceTest {

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

    private static final String EVENT_ID = "test-event-123";
    private static final String EMAIL = "test@example.com";
    private static final String NORM = "test@example.com";

    // ---------- helpers ----------

    private void mockTransactionGet(DocumentSnapshot snapshot) throws FirebaseFirestoreException {
        when(mockTransaction.get(any(DocumentReference.class))).thenReturn(snapshot);
    }

    private void mockSuccessfulTransaction() {
        doAnswer(inv -> {
            Transaction.Function<?> f = inv.getArgument(0);
            f.apply(mockTransaction);
            return Tasks.forResult(null);
        }).when(mockFirestore).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Use the REAL Event object here so we don't have to guess its getters/setters.
     * (All behavior comes from production code; we only mock Firestore around it.)
     */
    private Event event() {
        Event e = new Event("Test Event");
        e.setEventId(EVENT_ID);
        e.setCapacity(10);
        e.setWaitlistLimit(20);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date(System.currentTimeMillis() + 86400000));
        return e;
    }

    // ---------- setup ----------

    @Before
    public void setUp() throws FirebaseFirestoreException {
        MockitoAnnotations.initMocks(this);

        eventService = new EventService(mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(anyString())).thenReturn(mockProfileDocumentRef);
        when(mockProfileDocumentRef.collection("History")).thenReturn(mockHistoryCollection);
        when(mockHistoryCollection.document()).thenReturn(mockHistoryDocumentRef);

        when(mockTransaction.get(mockProfileDocumentRef)).thenReturn(mockProfileDocumentSnapshot);
        when(mockProfileDocumentSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);
    }

    // ---------- tests ----------

    @Test
    public void emailNormalization_AddsNormalizedEmail() throws Exception {
        Event e = event();

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        String messy = "   test@example.com   ";
        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, messy);
        Tasks.await(task);

        assertTrue(e.getWaitingList().contains(NORM));
        assertFalse(e.getWaitingList().contains(messy));
    }

    @Test
    public void emailNormalization_Removal() throws Exception {
        Event e = event();
        e.joinWaitingList(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        String messy = "   test@example.com   ";
        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, messy);
        Tasks.await(task);

        assertFalse(e.getWaitingList().contains(NORM));
    }

    @Test
    public void addEntrant_Fails_IfAlreadyInChosenList() throws Exception {
        Event e = event();
        e.addChosenEntrant(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);

        try {
            Tasks.await(task);
            fail();
        } catch (ExecutionException ex) {
            assertEquals("You have already been selected for this event.", ex.getCause().getMessage());
        }
    }

    @Test
    public void addEntrant_Fails_IfAlreadyInPendingList() throws Exception {
        Event e = event();
        e.addPendingEntrant(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);

        try {
            Tasks.await(task);
            fail();
        } catch (ExecutionException ex) {
            assertEquals("You have already accepted an invitation for this event.", ex.getCause().getMessage());
        }
    }

    @Test
    public void waitlistFull_UsingWaitlistLimit() throws Exception {
        Event e = event();
        e.setWaitlistLimit(3);
        e.joinWaitingList("a");
        e.joinWaitingList("b");
        e.joinWaitingList("c");

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);

        try {
            Tasks.await(task);
            fail();
        } catch (ExecutionException ex) {
            assertEquals("This waiting list is full.", ex.getCause().getMessage());
        }
    }

    @Test
    public void eventFull_UsingPendingCapacity() throws Exception {
        Event e = event();
        e.setCapacity(3);
        e.addPendingEntrant("x");
        e.addPendingEntrant("y");
        e.addPendingEntrant("z");

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);

        try {
            Tasks.await(task);
            fail();
        } catch (ExecutionException ex) {
            assertEquals("This event is full.", ex.getCause().getMessage());
        }
    }

    @Test
    public void removeEntrant_OnlyRemovesSpecified() throws Exception {
        Event e = event();
        e.joinWaitingList("one");
        e.joinWaitingList("two");
        e.joinWaitingList(EMAIL);
        e.joinWaitingList("three");

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);
        Tasks.await(task);

        assertFalse(e.getWaitingList().contains(EMAIL));
        assertTrue(e.getWaitingList().contains("one"));
        assertTrue(e.getWaitingList().contains("two"));
        assertTrue(e.getWaitingList().contains("three"));
    }
}
