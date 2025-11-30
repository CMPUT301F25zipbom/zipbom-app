package com.example.code_zombom_app.Entrant;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Filter.EventFilter;
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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class EntrantMainModelFilterEventTest {

    @Mock FirebaseFirestore mockDb;
    @Mock CollectionReference mockEventsCollection;
    @Mock Task<QuerySnapshot> mockTask;
    @Mock QuerySnapshot mockSnapshot;
    @Mock QueryDocumentSnapshot mockDoc1;
    @Mock QueryDocumentSnapshot mockDoc2;

    @Mock Event event1;
    @Mock Event event2;

    private EntrantMainModel model;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Use the constructor that accepts the mocked Firestore
        model = new EntrantMainModel("test@example.com", mockDb);

        // Common Firestore stubbing
        when(mockDb.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.get()).thenReturn(mockTask);

        // Make addOnSuccessListener immediately invoke the listener with our fake snapshot
        doAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));

        // Ensure addOnFailureListener still returns the task, even if unused
        when(mockTask.addOnFailureListener(any(OnFailureListener.class)))
                .thenReturn(mockTask);

        // Snapshot iteration will yield our two fake documents
        List<QueryDocumentSnapshot> docs = Arrays.asList(mockDoc1, mockDoc2);
        when(mockSnapshot.iterator()).thenReturn(docs.iterator());

        // Map docs → mocked Event objects
        when(mockDoc1.toObject(Event.class)).thenReturn(event1);
        when(mockDoc2.toObject(Event.class)).thenReturn(event2);
    }

    @Test
    public void filterEvent_appliesEventFilterAndStoresMatchingEvents() {
        // Arrange: configure filter and event fields so only event1 matches
        EventFilter filter = new EventFilter();
        filter.setFilterGenre("Music");

        Date filterStart = new Date(1700000000000L); // any consistent timestamps
        Date filterEnd   = new Date(1700500000000L);
        filter.setFilterStartDate(filterStart);
        filter.setFilterEndDate(filterEnd);

        // event1: overlapping dates & matching genre
        when(event1.getGenre()).thenReturn("Music");
        when(event1.getEventStartDate()).thenReturn(new Date(1700100000000L));
        when(event1.getEventEndDate()).thenReturn(new Date(1700400000000L));

        // event2: different genre → should be filtered out
        when(event2.getGenre()).thenReturn("Sport");
        // No need to stub start/end dates for event2 – they are never used

        // Act
        model.filterEvent(filter);

        // Assert
        List<Event> result = model.getLoadedEvents();
        assertEquals(1, result.size());
        assertSame(event1, result.get(0));
    }
}
