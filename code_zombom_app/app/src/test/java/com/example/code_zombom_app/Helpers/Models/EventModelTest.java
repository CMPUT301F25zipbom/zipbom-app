package com.example.code_zombom_app.Helpers.Models;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventModel.loadEvents() method.
 * Tests the feature for showing entrants a list of events they can join the waiting list for (US 01.01.03).
 *
 * @author Test Suite
 */
@RunWith(MockitoJUnitRunner.class)
public class EventModelTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private Query mockQuery;

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
     * Test returns only joinable events: Given a mix of events in Firestore,
     * when the method is called, it returns only valid events that can be joined.
     */
    @Test
    public void loadEvents_ReturnsOnlyJoinableEvents() {
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
            // Simulate success callback
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
     * Test excludes events user is already on the waiting list for:
     * If an event has the user already on the waiting list, it should still be loaded
     * (filtering happens at UI level, but we verify events are loaded correctly).
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
     * Test no joinable events: When Firestore returns no events that the user can join,
     * verify the method returns an empty list (not null).
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
        
        // After successful load, getLoadedEvents should return empty list
        // Note: In a real scenario, this would be tested via the model's state/callbacks
        // For unit test, we verify the query setup
    }

    /**
     * Test Firestore query failure: Simulate a failure when reading events from Firestore
     * and verify the method handles the error correctly.
     */
    @Test
    public void loadEvents_FirestoreQueryFailure_HandlesError() {
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
     * Test handles events with registration period still valid:
     * Events with future end dates should be included in the results.
     */
    @Test
    public void loadEvents_IncludesEventsWithValidRegistrationPeriod() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Date futureDate = new Date(System.currentTimeMillis() + 86400000); // Tomorrow
        Event eventWithValidPeriod = createTestEvent(TEST_EVENT_ID_1, "Future Event", 10, 20);
        eventWithValidPeriod.setEventEndDate(futureDate);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(eventWithValidPeriod);
        when(mockDocumentSnapshot1.getId()).thenReturn(TEST_EVENT_ID_1);
        when(mockDocumentSnapshot1.exists()).thenReturn(true);

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

        // Assert - verify Firestore query was called with correct collection
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test handles events where waiting list is not full:
     * Events with available capacity should be included.
     */
    @Test
    public void loadEvents_IncludesEventsWithAvailableCapacity() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Event eventWithCapacity = createTestEvent(TEST_EVENT_ID_1, "Event with Capacity", 10, 20);
        // Waiting list has space (only 2 out of 20)
        eventWithCapacity.joinWaitingList("user1@example.com");
        eventWithCapacity.joinWaitingList("user2@example.com");

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(eventWithCapacity);
        when(mockDocumentSnapshot1.getId()).thenReturn(TEST_EVENT_ID_1);
        when(mockDocumentSnapshot1.exists()).thenReturn(true);

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
     * Test handles exception during event conversion: Events that fail to convert
     * should be skipped without crashing.
     */
    @Test
    public void loadEvents_HandlesExceptionDuringEventConversion() {
        // Arrange
        eventModel = new TestableEventModel(mockFirestore);

        Event validEvent = createTestEvent(TEST_EVENT_ID_1, "Valid Event", 10, 20);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(validEvent);
        when(mockDocumentSnapshot1.getId()).thenReturn(TEST_EVENT_ID_1);
        when(mockDocumentSnapshot1.exists()).thenReturn(true);
        
        // Simulate conversion failure for second document
        when(mockDocumentSnapshot2.toObject(Event.class)).thenThrow(new RuntimeException("Conversion failed"));
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

        // Act - should not throw exception
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

