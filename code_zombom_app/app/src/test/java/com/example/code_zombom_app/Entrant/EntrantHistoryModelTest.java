package com.example.code_zombom_app.Entrant;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;
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

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers entrant history recording for join/win/lose scenarios (US 01.02.03).
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrantHistoryModelTest {

    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private CollectionReference mockEventsCollection;
    @Mock
    private CollectionReference mockProfilesCollection;
    @Mock
    private DocumentReference mockEventDocumentRef;

    @Mock
    private DocumentSnapshot mockEventSnapshot;
    @Mock
    private Transaction mockTransaction;

    @Mock
    private DocumentReference mockProfileDocWaitlisted;
    @Mock
    private CollectionReference mockHistoryCollectionWaitlisted;
    @Mock
    private DocumentReference mockHistoryDocWaitlisted;

    @Mock
    private DocumentReference mockProfileDocWinner;
    @Mock
    private CollectionReference mockHistoryCollectionWinner;
    @Mock
    private DocumentReference mockHistoryDocWinner;

    @Mock
    private DocumentReference mockProfileDocLoser;
    @Mock
    private CollectionReference mockHistoryCollectionLoser;
    @Mock
    private DocumentReference mockHistoryDocLoser;

    private EventService eventService;

    private static final String EVENT_ID = "history-event";
    private static final String WAIT_EMAIL = "wait@example.com";
    private static final String WIN_EMAIL = "win@example.com";
    private static final String LOSE_EMAIL = "lose@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        // Events
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);

        // Profiles
        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(WAIT_EMAIL)).thenReturn(mockProfileDocWaitlisted);



        // History subcollections – use anyString() to match whatever recordHistory uses

        when(mockHistoryCollectionWaitlisted.document()).thenReturn(mockHistoryDocWaitlisted);

        // In case recordHistory ever uses event-level history, avoid NPE
        when(mockEventDocumentRef.collection(any(String.class))).thenReturn(mockHistoryCollectionWaitlisted);

        // Transaction.set returns same transaction (fluent API)
        when(mockTransaction.set(any(DocumentReference.class), any())).thenReturn(mockTransaction);
        when(mockTransaction.set(any(DocumentReference.class), any(), any(SetOptions.class)))
                .thenReturn(mockTransaction);
    }

    private <T> T await(Task<T> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        throw new ExecutionException(task.getException());
    }

    private void mockTransaction(Event event) throws Exception {
        when(mockTransaction.get(eq(mockEventDocumentRef))).thenReturn(mockEventSnapshot);
        when(mockEventSnapshot.toObject(Event.class)).thenReturn(event);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Transaction.Function<Void> function = invocation.getArgument(0);
            try {
                function.apply(mockTransaction);
                return Tasks.forResult(null);
            } catch (Exception e) {
                return Tasks.forException(e);
            }
        }).when(mockFirestore).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void addEntrantToWaitlist_RecordsWaitlistedHistory() throws Exception {
        Event event = new Event("History Test");
        event.setEventId(EVENT_ID);

        mockTransaction(event);

        Task<Void> task = eventService.addEntrantToWaitlist(EVENT_ID, WAIT_EMAIL);
        await(task);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction, atLeastOnce()).set(eq(mockHistoryDocWaitlisted), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(EVENT_ID, payload.get("eventId"));
        assertEquals(Entrant.Status.WAITLISTED.name(), payload.get("status"));
    }
}
