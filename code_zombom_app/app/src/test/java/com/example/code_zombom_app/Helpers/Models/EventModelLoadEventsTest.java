package com.example.code_zombom_app.Helpers.Models;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventModel.loadEvents()
 */
@RunWith(MockitoJUnitRunner.class)
public class EventModelLoadEventsTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockEventsCollection;
    @Mock private QuerySnapshot mockQuerySnapshot;

    @Mock private QueryDocumentSnapshot mockDoc1;
    @Mock private QueryDocumentSnapshot mockDoc2;

    private EventModel eventModel;

    // ---------- Testable subclass ----------
    private static class TestableEventModel extends EventModel {
        TestableEventModel(FirebaseFirestore firestore) {
            super(firestore);  // IMPORTANT
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);

        eventModel = new TestableEventModel(mockFirestore);
    }

    // Helper — create a Task that triggers success
    private Task<QuerySnapshot> mockSuccessTask(QuerySnapshot snapshot) {
        Task<QuerySnapshot> task = mock(Task.class);

        when(mockEventsCollection.get()).thenReturn(task);

        // addOnSuccessListener
        doAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(snapshot);
            return task;
        }).when(task).addOnSuccessListener(any(OnSuccessListener.class));

        // addOnFailureListener
        doAnswer(inv -> task).when(task).addOnFailureListener(any(OnFailureListener.class));

        return task;
    }

    // Helper — create a Task that triggers failure
    private Task<QuerySnapshot> mockFailureTask(Exception e) {
        Task<QuerySnapshot> task = mock(Task.class);

        when(mockEventsCollection.get()).thenReturn(task);

        // Success listener → do nothing
        doAnswer(inv -> task).when(task).addOnSuccessListener(any(OnSuccessListener.class));

        // Failure listener → fire immediately
        doAnswer(inv -> {
            OnFailureListener listener = inv.getArgument(0);
            listener.onFailure(e);
            return task;
        }).when(task).addOnFailureListener(any(OnFailureListener.class));

        return task;
    }

    private Event makeEvent(String id) {
        Event e = new Event("Test");
        e.setEventId(id);
        e.setCapacity(10);
        e.setWaitlistLimit(20);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date(System.currentTimeMillis() + 86400000));
        return e;
    }

    // ---------------------------------------------------
    // TEST 1 — Normal load
    // ---------------------------------------------------
    @Test
    public void loadEvents_ReturnsValidEvents() {
        Event e1 = makeEvent("event1");
        Event e2 = makeEvent("event2");

        List<QueryDocumentSnapshot> docs = List.of(mockDoc1, mockDoc2);

        when(mockQuerySnapshot.iterator()).thenReturn(docs.iterator());
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);

        when(mockDoc1.exists()).thenReturn(true);
        when(mockDoc1.toObject(Event.class)).thenReturn(e1);

        when(mockDoc2.exists()).thenReturn(true);
        when(mockDoc2.toObject(Event.class)).thenReturn(e2);


        mockSuccessTask(mockQuerySnapshot);

        eventModel.loadEvents();

        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    // ---------------------------------------------------
    // TEST 2 — Empty result
    // ---------------------------------------------------
    @Test
    public void loadEvents_EmptyCollection() {
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        when(mockQuerySnapshot.iterator()).thenReturn(new ArrayList<QueryDocumentSnapshot>().iterator());

        mockSuccessTask(mockQuerySnapshot);

        eventModel.loadEvents();

        verify(mockEventsCollection).get();
    }

    // ---------------------------------------------------
    // TEST 3 — Firestore failure
    // ---------------------------------------------------
    @Test
    public void loadEvents_FirestoreFailure() {
        mockFailureTask(new Exception("Failed"));

        eventModel.loadEvents();

        verify(mockEventsCollection).get();
    }

    // ---------------------------------------------------
    // TEST 4 — Filters invalid events
    // ---------------------------------------------------
    @Test
    public void loadEvents_FiltersInvalidEvents() {

        Event valid = makeEvent("valid");
        Event invalid = new Event("Invalid");
        invalid.setEventId(null);

        List<QueryDocumentSnapshot> docs = List.of(mockDoc1, mockDoc2);

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.iterator()).thenReturn(docs.iterator());

        when(mockDoc1.exists()).thenReturn(true);
        when(mockDoc1.toObject(Event.class)).thenReturn(valid);

        when(mockDoc2.exists()).thenReturn(true);
        when(mockDoc2.toObject(Event.class)).thenReturn(invalid);

        mockSuccessTask(mockQuerySnapshot);

        eventModel.loadEvents();

        verify(mockEventsCollection).get();
    }

    // ---------------------------------------------------
    // TEST 5 — correct Firestore path
    // ---------------------------------------------------
    @Test
    public void loadEvents_CorrectPathUsed() {
        mockSuccessTask(mockQuerySnapshot);

        eventModel.loadEvents();

        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }
}
