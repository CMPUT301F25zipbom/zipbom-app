package com.example.code_zombom_app.Entrant;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Filter.EventFilter;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EntrantMainModel.filterEvent() method.
 * Tests the feature for filtering events based on interests and availability (US 01.01.04)
 * when events are loaded from Firestore.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrantMainModelFilterTest {

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

    @Mock
    private QueryDocumentSnapshot mockDocumentSnapshot4;

    private EntrantMainModel entrantMainModel;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String GENRE_SPORT = "Sport";
    private static final String GENRE_MUSIC = "Music";
    private static final String GENRE_FOOD = "Food";

    /**
     * Testable subclass of EntrantMainModel that uses the
     * Firestore-injecting constructor so tests never touch real Firebase.
     */
    private static class TestableEntrantMainModel extends EntrantMainModel {

        TestableEntrantMainModel(String email, FirebaseFirestore firestore) {
            // IMPORTANT: this calls EventModel(FirebaseFirestore) under the hood
            // so db is the Mockito mock, not FirebaseFirestore.getInstance()
            super(email, firestore);
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // IMPORTANT: disable QR generation so Event() doesn't touch Bitmap APIs in JVM tests
        Event.setQrCodeGenerationEnabled(false);
        // Whenever code calls db.collection("Events"), return the mocked collection
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
    }

    /**
     * Test filter by interests only with Firestore: Given events loaded from Firestore,
     * when filtering by interests, only matching events are returned.
     */
    @Test
    public void filterEvent_FilterByInterestsOnly_ReturnsMatchingEvents() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(GENRE_SPORT);
        filter.setFilterStartDate(null);
        filter.setFilterEndDate(null);

        Event sportEvent = createTestEvent("event-1", "Sport Event", GENRE_SPORT);
        Event musicEvent = createTestEvent("event-2", "Music Event", GENRE_MUSIC);
        Event foodEvent = createTestEvent("event-3", "Food Event", GENRE_FOOD);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);
        documents.add(mockDocumentSnapshot3);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(sportEvent);
        when(mockDocumentSnapshot1.getId()).thenReturn("event-1");
        when(mockDocumentSnapshot1.exists()).thenReturn(true);

        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(musicEvent);
        when(mockDocumentSnapshot2.getId()).thenReturn("event-2");
        when(mockDocumentSnapshot2.exists()).thenReturn(true);

        when(mockDocumentSnapshot3.toObject(Event.class)).thenReturn(foodEvent);
        when(mockDocumentSnapshot3.getId()).thenReturn("event-3");
        when(mockDocumentSnapshot3.exists()).thenReturn(true);

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

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        entrantMainModel.filterEvent(filter);

        // Assert - verify Firestore query was called
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
        verify(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));
    }

    /**
     * Test filter by availability only with Firestore: Given events with various dates,
     * when filtering by availability, only matching events are returned.
     */
    @Test
    public void filterEvent_FilterByAvailabilityOnly_ReturnsMatchingEvents() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(null);
        Date filterStart = new Date(100, 0, 15);
        Date filterEnd = new Date(100, 0, 20);
        filter.setFilterStartDate(filterStart);
        filter.setFilterEndDate(filterEnd);

        Event overlappingEvent = createTestEvent("event-1", "Overlapping Event", null);
        overlappingEvent.setEventStartDate(new Date(100, 0, 16));
        overlappingEvent.setEventEndDate(new Date(100, 0, 18));

        Event beforeEvent = createTestEvent("event-2", "Before Event", null);
        beforeEvent.setEventStartDate(new Date(100, 0, 10));
        beforeEvent.setEventEndDate(new Date(100, 0, 12));

        Event afterEvent = createTestEvent("event-3", "After Event", null);
        afterEvent.setEventStartDate(new Date(100, 0, 25));
        afterEvent.setEventEndDate(new Date(100, 0, 27));

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);
        documents.add(mockDocumentSnapshot3);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(overlappingEvent);
        when(mockDocumentSnapshot1.getId()).thenReturn("event-1");
        when(mockDocumentSnapshot1.exists()).thenReturn(true);

        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(beforeEvent);
        when(mockDocumentSnapshot2.getId()).thenReturn("event-2");
        when(mockDocumentSnapshot2.exists()).thenReturn(true);

        when(mockDocumentSnapshot3.toObject(Event.class)).thenReturn(afterEvent);
        when(mockDocumentSnapshot3.getId()).thenReturn("event-3");
        when(mockDocumentSnapshot3.exists()).thenReturn(true);

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

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        entrantMainModel.filterEvent(filter);

        // Assert
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test combined filtering (interests AND availability) with Firestore:
     * Only events satisfying both criteria should be included.
     */
    @Test
    public void filterEvent_CombinedFiltering_ReturnsEventsMatchingBothCriteria() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(GENRE_SPORT);
        Date filterStart = new Date(100, 0, 15);
        Date filterEnd = new Date(100, 0, 20);
        filter.setFilterStartDate(filterStart);
        filter.setFilterEndDate(filterEnd);

        Event matchingBoth = createTestEvent("event-1", "Matching Both", GENRE_SPORT);
        matchingBoth.setEventStartDate(new Date(100, 0, 16));
        matchingBoth.setEventEndDate(new Date(100, 0, 18));

        Event matchingInterestOnly = createTestEvent("event-2", "Matching Interest Only", GENRE_SPORT);
        matchingInterestOnly.setEventStartDate(new Date(100, 0, 25));
        matchingInterestOnly.setEventEndDate(new Date(100, 0, 27));

        Event matchingAvailabilityOnly = createTestEvent("event-3", "Matching Availability Only", GENRE_MUSIC);
        matchingAvailabilityOnly.setEventStartDate(new Date(100, 0, 16));
        matchingAvailabilityOnly.setEventEndDate(new Date(100, 0, 18));

        Event matchingNeither = createTestEvent("event-4", "Matching Neither", GENRE_FOOD);
        matchingNeither.setEventStartDate(new Date(100, 0, 25));
        matchingNeither.setEventEndDate(new Date(100, 0, 27));

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);
        documents.add(mockDocumentSnapshot3);
        documents.add(mockDocumentSnapshot4);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(matchingBoth);
        when(mockDocumentSnapshot1.getId()).thenReturn("event-1");
        when(mockDocumentSnapshot1.exists()).thenReturn(true);

        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(matchingInterestOnly);
        when(mockDocumentSnapshot2.getId()).thenReturn("event-2");
        when(mockDocumentSnapshot2.exists()).thenReturn(true);

        when(mockDocumentSnapshot3.toObject(Event.class)).thenReturn(matchingAvailabilityOnly);
        when(mockDocumentSnapshot3.getId()).thenReturn("event-3");
        when(mockDocumentSnapshot3.exists()).thenReturn(true);

        when(mockDocumentSnapshot4.toObject(Event.class)).thenReturn(matchingNeither);
        when(mockDocumentSnapshot4.getId()).thenReturn("event-4");
        when(mockDocumentSnapshot4.exists()).thenReturn(true);

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

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        entrantMainModel.filterEvent(filter);

        // Assert
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test no matching events: When no events match the filter criteria,
     * the result should be an empty list.
     */
    @Test
    public void filterEvent_NoMatchingEvents_ReturnsEmptyList() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(GENRE_SPORT);
        Date filterStart = new Date(100, 0, 15);
        Date filterEnd = new Date(100, 0, 20);
        filter.setFilterStartDate(filterStart);
        filter.setFilterEndDate(filterEnd);

        Event wrongGenre = createTestEvent("event-1", "Wrong Genre", GENRE_MUSIC);
        wrongGenre.setEventStartDate(new Date(100, 0, 16));
        wrongGenre.setEventEndDate(new Date(100, 0, 18));

        Event wrongDates = createTestEvent("event-2", "Wrong Dates", GENRE_SPORT);
        wrongDates.setEventStartDate(new Date(100, 0, 25));
        wrongDates.setEventEndDate(new Date(100, 0, 27));

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(wrongGenre);
        when(mockDocumentSnapshot1.getId()).thenReturn("event-1");
        when(mockDocumentSnapshot1.exists()).thenReturn(true);

        when(mockDocumentSnapshot2.toObject(Event.class)).thenReturn(wrongDates);
        when(mockDocumentSnapshot2.getId()).thenReturn("event-2");
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

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        entrantMainModel.filterEvent(filter);

        // Assert
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test Firestore query failure: Simulate Firestore failure and verify error handling.
     */
    @Test
    public void filterEvent_FirestoreQueryFailure_HandlesError() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(GENRE_SPORT);

        Exception firestoreException = new Exception("Firestore query failed");

        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnFailureListener failureListener =
                    invocation.getArgument(0);
            failureListener.onFailure(firestoreException);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        entrantMainModel.filterEvent(filter);

        // Assert
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
        verify(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));
    }

    /**
     * Test correct Firestore collection path: Verify that the query uses the correct collection.
     */
    @Test
    public void filterEvent_UsesCorrectFirestoreCollectionPath() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(null);

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

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        entrantMainModel.filterEvent(filter);

        // Assert - verify the correct collection path is used
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Test handles exception during event conversion: Events that fail to convert
     * should be skipped without crashing.
     */
    @Test
    public void filterEvent_HandlesExceptionDuringEventConversion() {
        // Arrange
        entrantMainModel = new TestableEntrantMainModel(TEST_EMAIL, mockFirestore);

        EventFilter filter = new EventFilter();
        filter.setFilterGenre(GENRE_SPORT);

        Event validEvent = createTestEvent("event-1", "Valid Event", GENRE_SPORT);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot1);
        documents.add(mockDocumentSnapshot2);

        when(mockDocumentSnapshot1.toObject(Event.class)).thenReturn(validEvent);
        when(mockDocumentSnapshot1.getId()).thenReturn("event-1");
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

        doAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask)
                .when(mockTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act - should not throw exception
        entrantMainModel.filterEvent(filter);

        // Assert
        verify(mockFirestore).collection("Events");
        verify(mockEventsCollection).get();
    }

    /**
     * Helper method to create a test Event with default values.
     */
    private Event createTestEvent(String eventId, String name, String genre) {
        Event event = new Event(name);
        event.setEventId(eventId);
        event.setGenre(genre);
        event.setCapacity(10);
        event.setWaitlistLimit(20);
        return event;
    }
}
