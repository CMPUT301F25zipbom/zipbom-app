package com.example.code_zombom_app.Helpers.Event;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceHistoryTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private com.google.firebase.firestore.CollectionReference mockEventsCollection;
    @Mock private com.google.firebase.firestore.CollectionReference mockProfilesCollection;
    @Mock private com.google.firebase.firestore.CollectionReference mockHistoryCollection;

    @Mock private DocumentReference mockEventDocumentRef;
    @Mock private DocumentReference mockProfileDocumentRef;
    @Mock private DocumentReference mockHistoryDocumentRef;

    @Mock private DocumentSnapshot mockEventDocumentSnapshot;
    @Mock private Transaction mockTransaction;

    private EventService eventService;

    private static final String EVENT_ID = "history-event-123";
    private static final String EMAIL = "test@example.com";
    private static final String NORM = "test@example.com";

    // ---------- helpers ----------

    private void mockTransactionGet(DocumentSnapshot snapshot) throws Exception {
        when(mockTransaction.get(any(DocumentReference.class))).thenReturn(snapshot);
    }

    /**
     * runTransaction stub that executes the lambda and:
     *  - returns a successful Task on success
     *  - returns a failed Task (Tasks.forException) if the lambda throws
     */
    private void mockSuccessfulTransaction() {
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Transaction.Function<Void> f = inv.getArgument(0);
            try {
                f.apply(mockTransaction);
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
        Exception e = task.getException();
        if (e != null) {
            throw new ExecutionException(e);
        }
        return null;
    }

    private Event event() {
        Event e = new Event("History Test Event");
        e.setEventId(EVENT_ID);
        e.setCapacity(10);
        e.setWaitlistLimit(20);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date(System.currentTimeMillis() + 86400000));
        return e;
    }

    // ---------- setup ----------

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Disable QR generation for unit tests
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        // Events collection + event doc
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);

        // Profiles collection + profile doc
        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(anyString())).thenReturn(mockProfileDocumentRef);

        // History subcollection (used by EventService.recordHistory)
        // Stub on both event + profile docs to be robust to implementation details
        when(mockEventDocumentRef.collection(anyString())).thenReturn(mockHistoryCollection);
        when(mockHistoryCollection.document()).thenReturn(mockHistoryDocumentRef);
    }

    // ---------- tests ----------

    @Test
    public void addEntrantToWaitlist_WritesHistoryOnSuccess() throws Exception {
        Event e = event();

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        // Event state updated
        verify(mockTransaction).set(eq(mockEventDocumentRef), any(Event.class));
        assertTrue(e.getWaitingList().contains(NORM));

        // History written
        verify(mockTransaction).set(eq(mockHistoryDocumentRef), any());
    }


    @Test
    public void removeEntrantFromWaitlist_WritesHistoryOnSuccess() throws Exception {
        Event e = event();
        e.joinWaitingList(NORM);

        mockTransactionGet(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        // Event state updated
        verify(mockTransaction).set(eq(mockEventDocumentRef), any(Event.class));
        assertFalse(e.getWaitingList().contains(NORM));

        // History written
        verify(mockTransaction).set(eq(mockHistoryDocumentRef), any());
    }


}
