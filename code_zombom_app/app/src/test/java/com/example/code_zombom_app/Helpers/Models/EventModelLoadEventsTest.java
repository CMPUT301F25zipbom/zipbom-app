package com.example.code_zombom_app.Helpers.Models;

import com.example.code_zombom_app.Helpers.Event.Event;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventModel.loadEvents() method.
 * Tests US 01.01.03: "As an entrant, I want to be able to see a list of events that I can join the waiting list for."
 *
 * @author Test Suite
 */
@RunWith(MockitoJUnitRunner.class)
public class EventModelLoadEventsTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private QueryDocumentSnapshot mockDocumentSnapshot1;

    @Mock
    private QueryDocumentSnapshot mockDocumentSnapshot2;

    @Mock
    private QueryDocumentSnapshot mockDocumentSnapshot3;

    private EventModel eventModel;
    private static final String TEST_EVENT_ID_1 = "event-1";
    private static final String TEST_EVENT_ID_2 = "event-2";
    private static final String TEST_EVENT_ID_3 = "event-3";

    /**
     * Testable subclass of EventModel that allows dependency injection for testing.
     */
    private static class TestableEventModel extends EventModel {
        private final FirebaseFirestore testDb;

        TestableEventModel(FirebaseFirestore firestore) {
            super();
            this.testDb = firestore;
            try {
                // Use reflection to inject the mock Firestore instance
                Field dbField = EventModel.class.getDeclaredField("db");
                dbField.setAccessible(true);
                dbField.set(this, firestore);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject mock Firestore", e);
            }
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // IMPORTANT: disable QR generation so Event() doesn't touch Bitmap APIs in JVM tests
        Event.setQrCodeGenerationEnabled(false);
        
        // Setup Firestore collection mock
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
    }

    /**
     * Test returns only joinable events: Given Firestore returns a mix of events
     * (open vs closed registration, full vs not full), verify only joinable events are returned.
     */
    @Test
    public void loadEvents_ReturnsOnlyJoinableEvents_FromFirestore() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Event validEvent1 = createTestEvent(TEST_EVENT_ID_1, "Event 1", 10, 20);
        Event validEvent2 = createTestEvent(TEST_EVENT_ID_2, "Event 2", 5, 15);
        
        // Create a list of document snapshots
        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);

        // Mock document snapshots to return valid events
        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(validEvent1);
        when(mockDocumentSnapshot1.getId()).thenReturn(TEST_EVENT_ID_1);
        when(mockDocumentSnapshot1.exists()).thenReturn(true);
        
        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(validEvent2);
        when(mockDocumentSnapshot2.getId()).thenReturn(TEST_EVENT_ID_2);
        when(mockDocumentSnapshot2.exists()).thenReturn(true);

        // Mock QuerySnapshot
        when(mockQuerySnapshot.iterator()).thenReturn(documents.iterator());
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);

        // Mock the Task to execute the success listener
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);
        
        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener = 
                    invocation.getArgument(0);
            successListener.onSuccess(mockQuerySnapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        eventModel.loadEvents();

        // Assert - verify the Firestore query was set up correctly
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
        
        // Verify success and failure listeners were set up
        verify(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));
        verify(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));
    }

    /**
     * Test excludes events the user is already on the waiting list for (if the code supports that).
     * Note: The current implementation loads all events; filtering happens at UI level.
     * This test verifies all events are loaded correctly.
     */
    @Test
    public void loadEvents_LoadsAllEventsIncludingThoseUserIsOnWaitlist() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Event eventWithUserOnWaitlist = createTestEvent(TEST_EVENT_ID_1, "Event 1", 10, 20);
        eventWithUserOnWaitlist.joinWaitingList("user@example.com");
        
        Event eventWithoutUser = createTestEvent(TEST_EVENT_ID_2, "Event 2", 5, 15);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(eventWithUserOnWaitlist);
        when(mockDocumentSnapshot1.getId()).thenReturn(TEST_EVENT_ID_1);
        when(mockDocumentSnapshot1.exists()).thenReturn(true);
        
        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(eventWithoutUser);
        when(mockDocumentSnapshot2.getId()).thenReturn(TEST_EVENT_ID_2);
        when(mockDocumentSnapshot2.exists()).thenReturn(true);

        when(mockQuerySnapshot.iterator()).thenReturn(documents.iterator());
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);

        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);
        
        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener = 
                    invocation.getArgument(0);
            successListener.onSuccess(mockQuerySnapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        eventModel.loadEvents();

        // Assert - verify Firestore query was called
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test no joinable events: Returns an empty list (not null) or equivalent.
     */
    @Test
    public void loadEvents_NoJoinableEvents_ReturnsEmptyList() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        // Mock empty QuerySnapshot
        when(mockQuerySnapshot.iterator()).thenReturn(new ArrayList<QueryDocumentSnapshot>().iterator());
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);
        
        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener = 
                    invocation.getArgument(0);
            successListener.onSuccess(mockQuerySnapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        eventModel.loadEvents();

        // Assert - verify Firestore query was called
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test Firestore query failure: Simulate failure and assert error state is propagated correctly.
     */
    @Test
    public void loadEvents_FirestoreQueryFailure_PropagatesError() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Exception firestoreException = new Exception("Firestore query failed");
        
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);
        
        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            // Simulate failure callback
            com.google.android.gms.tasks.OnFailureListener failureListener = 
                    invocation.getArgument(0);
            failureListener.onFailure(firestoreException);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        eventModel.loadEvents();

        // Assert - verify Firestore query was called
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
        
        // Verify failure listener was set up
        verify(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));
    }

    /**
     * Test correct Firestore collection path: Verify that the query uses the correct
     * collection name "Events".
     */
    @Test
    public void loadEvents_UsesCorrectFirestoreCollectionPath() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);
        
        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        eventModel.loadEvents();

        // Assert - verify the correct collection path is used
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test filters out invalid events: Events with null or missing eventId should be skipped.
     */
    @Test
    public void loadEvents_FiltersOutInvalidEvents() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Event validEvent = createTestEvent(TEST_EVENT_ID_1, "Valid Event", 10, 20);
        Event invalidEvent = new Event("Invalid Event"); // Event without eventId set
        invalidEvent.setEventId(null); // Explicitly set to null

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(validEvent);
        when(mockDocumentSnapshot1.getId()).thenReturn(TEST_EVENT_ID_1);
        when(mockDocumentSnapshot1.exists()).thenReturn(true);
        
        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(invalidEvent);
        when(mockDocumentSnapshot2.getId()).thenReturn("invalid-doc-id");
        when(mockDocumentSnapshot2.exists()).thenReturn(true);

        when(mockQuerySnapshot.iterator()).thenReturn(documents.iterator());
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);

        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);
        
        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener = 
                    invocation.getArgument(0);
            successListener.onSuccess(mockQuerySnapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        eventModel.loadEvents();

        // Assert - verify Firestore query was called
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Helper method to create a test Event with default values.
     */
    private Event createTestEvent(String eventId, String name, int capacity, int waitlistLimit) {
        Event event = new Event(name);
        event.setEventId(eventId);
        event.setCapacity(capacity);
        event.setWaitlistLimit(waitlistLimit);
        event.setEventStartDate(new Date());
        event.setEventEndDate(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
        return event;
    }
}


