package com.example.code_zombom_app.Helpers.Event;

import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional EventService tests focused on entrant history tracking (US 01.02.03).
 * Verifies that waitlist/leave flows write History subcollection entries and keep the
 * profile-level eventHistory map in sync, and that Firestore write failures propagate.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventServiceHistoryTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockEventsCollection;
    @Mock private CollectionReference mockProfilesCollection;
    @Mock private CollectionReference mockHistoryCollection;
    @Mock private DocumentReference mockEventDocumentRef;
    @Mock private DocumentReference mockProfileDocumentRef;
    @Mock private DocumentReference mockHistoryDocumentRef;
    @Mock private DocumentSnapshot mockEventDocumentSnapshot;
    @Mock private Transaction mockTransaction;

    private EventService eventService;

    private static final String EVENT_ID = "history-event";
    private static final String EMAIL = "entrant@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(EMAIL)).thenReturn(mockProfileDocumentRef);
        when(mockProfileDocumentRef.collection("History")).thenReturn(mockHistoryCollection);
        when(mockHistoryCollection.document()).thenReturn(mockHistoryDocumentRef);

        when(mockTransaction.set(any(DocumentReference.class), any())).thenReturn(mockTransaction);
        when(mockTransaction.set(any(DocumentReference.class), any(), any(SetOptions.class)))
                .thenReturn(mockTransaction);
    }

    /**
     * runTransaction stub mirroring EventServiceTest to synchronously execute functions.
     */
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

    private Event event() {
        Event e = new Event("History Test Event");
        e.setEventId(EVENT_ID);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date(System.currentTimeMillis() + 3600000));
        e.setCapacity(5);
        e.setWaitlistLimit(5);
        return e;
    }

    @Test
    public void addEntrantToWaitlist_WritesHistoryEntryAndProfileStatus() throws Exception {
        Event e = event();

        when(mockTransaction.get(mockEventDocumentRef)).thenReturn(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        // Verify History/{autoId} write
        ArgumentCaptor<Map<String, Object>> historyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction, atLeastOnce()).set(
                org.mockito.ArgumentMatchers.eq(mockHistoryDocumentRef),
                historyCaptor.capture());

        Map<String, Object> payload = historyCaptor.getValue();
        assertEquals("Event id stored in history", EVENT_ID, payload.get("eventId"));
        assertEquals("WAITLISTED recorded", Entrant.Status.WAITLISTED.name(), payload.get("status"));
        assertNotNull("updatedAt timestamp present", payload.get("updatedAt"));

        // Verify profile eventHistory map merge
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction, atLeastOnce()).set(
                org.mockito.ArgumentMatchers.eq(mockProfileDocumentRef),
                mapCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(SetOptions.merge()));

        Map<String, Object> historyMapUpdate = mapCaptor.getValue();
        assertEquals("WAITLISTED reflected in profile map",
                Entrant.Status.WAITLISTED.name(),
                historyMapUpdate.get("eventHistory." + EVENT_ID));
    }

    @Test
    public void removeEntrantFromWaitlist_WritesLeaveHistory() throws Exception {
        Event e = event();
        e.joinWaitingList(EMAIL);

        when(mockTransaction.get(mockEventDocumentRef)).thenReturn(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);
        mockSuccessfulTransaction();

        Task<Void> task = eventService.removeEntrantFromWaitlist(EVENT_ID, EMAIL);
        awaitTask(task);

        ArgumentCaptor<Map<String, Object>> historyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction, atLeastOnce()).set(
                org.mockito.ArgumentMatchers.eq(mockHistoryDocumentRef),
                historyCaptor.capture());

        assertEquals("LEAVE recorded",
                Entrant.Status.LEAVE.name(),
                historyCaptor.getValue().get("status"));
    }

    @Test
    public void addEntrantToWaitlist_HistoryWriteFailure_PropagatesException() throws Exception {
        Event e = event();

        when(mockTransaction.get(mockEventDocumentRef)).thenReturn(mockEventDocumentSnapshot);
        when(mockEventDocumentSnapshot.toObject(Event.class)).thenReturn(e);

        // ✅ Use a RuntimeException instead of FirebaseFirestoreException
        RuntimeException historyFailure = new RuntimeException("history write failed");

        when(mockTransaction.set(
                org.mockito.ArgumentMatchers.eq(mockHistoryDocumentRef),
                any()))
                .thenThrow(historyFailure);

        mockSuccessfulTransaction();

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, EMAIL);

        try {
            awaitTask(task);
            fail("Expected history failure to bubble up");
        } catch (ExecutionException ex) {
            assertEquals("history write failed", ex.getCause().getMessage());
        }
    }
}
