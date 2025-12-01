//package com.example.code_zombom_app.Organizer;
//
//import com.example.code_zombom_app.Helpers.Event.Event;
//import com.example.code_zombom_app.Helpers.Event.EventService;
//import com.google.android.gms.tasks.Task;
//import com.google.android.gms.tasks.Tasks;
//import com.google.firebase.firestore.CollectionReference;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.Transaction;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.util.Map;
//import java.util.concurrent.ExecutionException;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.atLeastOnce;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@RunWith(MockitoJUnitRunner.class)
//public class OrganizerNotificationsTest {
//
//    @Mock private FirebaseFirestore mockFirestore;
//    @Mock private CollectionReference mockEventsCollection;
//    @Mock private CollectionReference mockNotificationPrefsCollection;
//    @Mock private CollectionReference mockNotificationsCollection;
//    @Mock private DocumentReference mockEventDocumentRef;
//    @Mock private DocumentReference mockNotificationDocRef1;
//    @Mock private DocumentReference mockNotificationDocRef2;
//    @Mock private DocumentSnapshot mockEventSnapshot;
//    @Mock private DocumentSnapshot mockPrefSnapshotEnabled;
//    @Mock private DocumentSnapshot mockPrefSnapshotDisabled;
//    @Mock private Transaction mockTransaction;
//
//    private EventService eventService;
//
//    private static final String EVENT_ID = "org-notify-event";
//    private static final String WAIT_1 = "wait1@example.com";
//    private static final String WAIT_2 = "wait2@example.com";
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        Event.setQrCodeGenerationEnabled(false);
//
//        eventService = new EventService(mockFirestore);
//
//        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
//        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
//
//        when(mockEventDocumentRef.collection("Notifications")).thenReturn(mockNotificationsCollection);
//        when(mockNotificationsCollection.document())
//                .thenReturn(mockNotificationDocRef1, mockNotificationDocRef2);
//
//        when(mockFirestore.collection("NotificationPreferences"))
//                .thenReturn(mockNotificationPrefsCollection);
//    }
//
//    private void mockRunTransaction(Event event) {
//        when(mockEventSnapshot.toObject(Event.class)).thenReturn(event);
//
//        doReturn(mockEventSnapshot)
//                .when(mockTransaction)
//                .get(eq(mockEventDocumentRef));
//
//        doAnswer(invocation -> {
//            Transaction.Function<Void> fn = invocation.getArgument(0);
//            try {
//                fn.apply(mockTransaction);
//                return Tasks.forResult(null);
//            } catch (Exception e) {
//                return Tasks.forException(e);
//            }
//        }).when(mockFirestore).runTransaction(any(Transaction.Function.class));
//    }
//
//    private <T> T awaitTask(Task<T> task) throws ExecutionException {
//        if (task.isSuccessful()) {
//            return task.getResult();
//        }
//        throw new ExecutionException(task.getException());
//    }
//
//    @Test
//    public void notifyWaitlistEntrants_SendsToAllEnabledRecipients() throws Exception {
//        Event event = new Event("Notify Waitlist");
//        event.setEventId(EVENT_ID);
//        event.joinWaitingList(WAIT_1);
//        event.joinWaitingList(WAIT_2);
//
//        DocumentReference prefRef1 = mock(DocumentReference.class);
//        DocumentReference prefRef2 = mock(DocumentReference.class);
//
//        when(mockNotificationPrefsCollection.document(WAIT_1)).thenReturn(prefRef1);
//        when(mockNotificationPrefsCollection.document(WAIT_2)).thenReturn(prefRef2);
//
//        doReturn(mockPrefSnapshotEnabled).when(mockTransaction).get(eq(prefRef1));
//        doReturn(mockPrefSnapshotDisabled).when(mockTransaction).get(eq(prefRef2));
//
//        when(mockPrefSnapshotEnabled.exists()).thenReturn(true);
//        when(mockPrefSnapshotEnabled.getBoolean("notificationEnabled")).thenReturn(true);
//
//        when(mockPrefSnapshotDisabled.exists()).thenReturn(true);
//        when(mockPrefSnapshotDisabled.getBoolean("notificationEnabled")).thenReturn(false);
//
//        mockRunTransaction(event);
//
//        Task<Void> task = eventService.notifyWaitlistEntrants(EVENT_ID, "Organizer update");
//        awaitTask(task);
//
//        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
//        verify(mockTransaction, atLeastOnce()).set(eq(mockNotificationDocRef1), payloadCaptor.capture());
//
//        Map<String, Object> payload = payloadCaptor.getValue();
//        assertNotNull(payload);
//        assertEquals("wait1@example.com", payload.get("recipientEmail"));
//        assertEquals("org_waitlist", payload.get("type"));
//    }
//
//    @Test
//    public void notifySelectedEntrants_SendsToChosenAndPending() throws Exception {
//        Event event = new Event("Notify Selected");
//        event.setEventId(EVENT_ID);
//
//        String chosenEmail = "chosen@example.com";
//        String pendingEmail = "pending@example.com";
//
//        event.addChosenEntrant(chosenEmail);
//        event.addPendingEntrant(pendingEmail);
//
//        DocumentReference prefChosen = mock(DocumentReference.class);
//        DocumentReference prefPending = mock(DocumentReference.class);
//
//        when(mockNotificationPrefsCollection.document(chosenEmail)).thenReturn(prefChosen);
//        when(mockNotificationPrefsCollection.document(pendingEmail)).thenReturn(prefPending);
//
//        doReturn(mockPrefSnapshotEnabled).when(mockTransaction).get(eq(prefChosen));
//        doReturn(mockPrefSnapshotEnabled).when(mockTransaction).get(eq(prefPending));
//
//        when(mockPrefSnapshotEnabled.exists()).thenReturn(true);
//        when(mockPrefSnapshotEnabled.getBoolean("notificationEnabled")).thenReturn(true);
//
//        when(mockNotificationsCollection.document())
//                .thenReturn(mockNotificationDocRef1)
//                .thenReturn(mockNotificationDocRef2);
//
//        mockRunTransaction(event);
//
//        Task<Void> task = eventService.notifySelectedEntrants(EVENT_ID, "Selected update");
//        awaitTask(task);
//
//        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
//
//        verify(mockTransaction, atLeastOnce()).set(eq(mockNotificationDocRef1), payloadCaptor.capture());
//        verify(mockTransaction, atLeastOnce()).set(eq(mockNotificationDocRef2), payloadCaptor.capture());
//
//        Map<String, Object> p1 = payloadCaptor.getAllValues().get(0);
//        Map<String, Object> p2 = payloadCaptor.getAllValues().get(1);
//
//        assertNotNull(p1);
//        assertNotNull(p2);
//
//        assertEquals("org_selected", p1.get("type"));
//        assertEquals("org_selected", p2.get("type"));
//    }
//
//    @Test
//    public void notifyCancelledEntrants_SendsToCancelledList() throws Exception {
//        Event event = new Event("Notify Cancelled");
//        event.setEventId(EVENT_ID);
//        event.addCancelledEntrant("cancel1@example.com");
//        event.addCancelledEntrant("cancel2@example.com");
//
//        mockRunTransaction(event);
//
//        Task<Void> task = eventService.notifyCancelledEntrants(EVENT_ID, "Cancelled update");
//        awaitTask(task);
//
//        verify(mockTransaction, atLeastOnce()).set(eq(mockNotificationDocRef1), any(Map.class));
//        verify(mockTransaction, atLeastOnce()).set(eq(mockNotificationDocRef2), any(Map.class));
//    }
//}
