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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies entrant notifications for lottery winners/losers and opt-out preferences (US 01.04.01, US 01.04.02, US 01.04.03).
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrantNotificationServiceTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockEventsCollection;
    @Mock private DocumentReference mockEventDocumentRef;
    @Mock private DocumentSnapshot mockEventSnapshot;
    @Mock private Transaction mockTransaction;

    @Mock private CollectionReference mockNotificationCollection;
    @Mock private DocumentReference mockWinnerNotificationDoc;
    @Mock private DocumentReference mockLoserNotificationDoc;

    @Mock private CollectionReference mockNotificationPrefsCollection;
    @Mock private DocumentReference mockWinnerPrefDoc;
    @Mock private DocumentReference mockLoserPrefDoc;

    @Mock private DocumentSnapshot mockWinnerPrefSnapshot;
    @Mock private DocumentSnapshot mockLoserPrefSnapshot;

    private EventService eventService;

    private static final String EVENT_ID = "notify-event";
    private static final String WIN_EMAIL = "winner@example.com";
    private static final String LOSE_EMAIL = "loser@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);

        when(mockEventDocumentRef.collection("Notifications")).thenReturn(mockNotificationCollection);
        when(mockNotificationCollection.document()).thenReturn(mockWinnerNotificationDoc, mockLoserNotificationDoc);

        when(mockFirestore.collection("NotificationPreferences")).thenReturn(mockNotificationPrefsCollection);
        when(mockNotificationPrefsCollection.document(WIN_EMAIL)).thenReturn(mockWinnerPrefDoc);
        when(mockNotificationPrefsCollection.document(LOSE_EMAIL)).thenReturn(mockLoserPrefDoc);
    }

    private void configureTransaction(Event event) throws FirebaseFirestoreException {
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
    public void runLotteryDraw_WinnerReceivesWinNotification() throws Exception {
        Event event = new Event("Notification Test");
        event.setEventId(EVENT_ID);
        event.joinWaitingList(WIN_EMAIL);
        event.setCapacity(1);
        when(mockNotificationCollection.document()).thenReturn(mockWinnerNotificationDoc, mockLoserNotificationDoc);

        when(mockTransaction.get(eq(mockWinnerPrefDoc))).thenReturn(mockWinnerPrefSnapshot);
        when(mockWinnerPrefSnapshot.exists()).thenReturn(true);
        when(mockWinnerPrefSnapshot.getBoolean("notificationEnabled")).thenReturn(true);

        configureTransaction(event);

        Task<Void> task = eventService.runLotteryDraw(EVENT_ID);
        await(task);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction, atLeastOnce()).set(eq(mockWinnerNotificationDoc), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("win", payload.get("type"));
        assertEquals(WIN_EMAIL, payload.get("recipientEmail"));
    }

    @Test
    public void runLotteryDraw_LoserOptOutSkipsNotification() throws Exception {
        Event event = new Event("Opt Out Test");
        event.setEventId(EVENT_ID);
        event.joinWaitingList(WIN_EMAIL);
        event.joinWaitingList(LOSE_EMAIL);
        event.setCapacity(1);
        when(mockNotificationCollection.document()).thenReturn(mockWinnerNotificationDoc, mockLoserNotificationDoc);

        when(mockTransaction.get(eq(mockWinnerPrefDoc))).thenReturn(mockWinnerPrefSnapshot);
        when(mockWinnerPrefSnapshot.exists()).thenReturn(true);
        when(mockWinnerPrefSnapshot.getBoolean("notificationEnabled")).thenReturn(true);

        when(mockTransaction.get(eq(mockLoserPrefDoc))).thenReturn(mockLoserPrefSnapshot);
        when(mockLoserPrefSnapshot.exists()).thenReturn(true);
        when(mockLoserPrefSnapshot.getBoolean("notificationEnabled")).thenReturn(false);

        configureTransaction(event);

        Task<Void> task = eventService.runLotteryDraw(EVENT_ID);
        await(task);

        verify(mockTransaction, atLeastOnce()).set(eq(mockWinnerNotificationDoc), any(Map.class));
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction).set(eq(mockWinnerNotificationDoc), payloadCaptor.capture());
        Map<String, Object> winnerPayload = payloadCaptor.getValue();
        assertEquals("win", winnerPayload.get("type"));
        // Loser opted out -> notification doc never written
        verify(mockTransaction, never()).set(eq(mockLoserNotificationDoc), any(Map.class));
    }

    @Test
    public void runLotteryDraw_LoserReceivesLoseNotification() throws Exception {
        Event event = new Event("Lose Notification Test");
        event.setEventId(EVENT_ID);
        event.joinWaitingList(WIN_EMAIL);
        event.joinWaitingList(LOSE_EMAIL);
        event.setCapacity(1);
        when(mockNotificationCollection.document()).thenReturn(mockWinnerNotificationDoc, mockLoserNotificationDoc);

        when(mockTransaction.get(eq(mockWinnerPrefDoc))).thenReturn(mockWinnerPrefSnapshot);
        when(mockWinnerPrefSnapshot.exists()).thenReturn(true);
        when(mockWinnerPrefSnapshot.getBoolean("notificationEnabled")).thenReturn(true);

        when(mockTransaction.get(eq(mockLoserPrefDoc))).thenReturn(mockLoserPrefSnapshot);
        when(mockLoserPrefSnapshot.exists()).thenReturn(true);
        when(mockLoserPrefSnapshot.getBoolean("notificationEnabled")).thenReturn(true);

        configureTransaction(event);

        Task<Void> task = eventService.runLotteryDraw(EVENT_ID);
        await(task);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTransaction, atLeastOnce()).set(eq(mockLoserNotificationDoc), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("lose", payload.get("type"));
        assertEquals(LOSE_EMAIL, payload.get("recipientEmail"));
    }
}


