// File: app/src/test/java/com/example/code_zombom_app/organizer/OrganizerEventCreationTest.java
package com.example.code_zombom_app.organizer;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.Helpers.Location.Location;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Organizer-focused tests for creating events and generating QR payloads (US 02.01.01).
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizerEventCreationTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private DocumentReference mockEventDocumentRef;

    private EventService eventService;

    private static final String EVENT_ID = "org-create-event-1";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Event.setQrCodeGenerationEnabled(false);

        eventService = new EventService(mockFirestore);
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
    }

    @Test
    public void newEvent_HasUniqueIdAndEmptyLists() {
        Event e1 = new Event("Organizer Test Event 1");
        Event e2 = new Event("Organizer Test Event 2");

        assertNotNull("Event 1 id should not be null", e1.getEventId());
        assertNotNull("Event 2 id should not be null", e2.getEventId());

        assertTrue("Event ids should be different for separate events",
                !e1.getEventId().equals(e2.getEventId()));

        assertEquals(0, e1.getWaitingList().size());
        assertEquals(0, e1.getChosenList().size());
        assertEquals(0, e1.getPendingList().size());
        assertEquals(0, e1.getRegisteredList().size());
        assertEquals(0, e1.getCancelledList().size());
    }

    @Test
    public void buildQrPayload_IncludesNameLocationDatesDescriptionAndPoster() {
        Event event = new Event("QR Test Event");
        event.setDescription("QR description");

        Location loc = new Location();
        loc.setName("Test Hall");
        event.setLocation(loc);

        Date start = new Date(System.currentTimeMillis() - 3600000);
        Date end = new Date(System.currentTimeMillis() + 3600000);
        event.setEventStartDate(start);
        event.setEventEndDate(end);

        String posterUrl = "https://example.com/poster.png";

        String payload = EventService.buildQrPayload(event, posterUrl);

        assertTrue(payload.contains("QR Test Event"));
        assertTrue(payload.contains("Test Hall"));
        assertTrue(payload.contains(event.getEventStartDate().toString()));
        assertTrue(payload.contains(event.getEventEndDate().toString()));
        assertTrue(payload.contains("QR description"));
        assertTrue(payload.contains(posterUrl));
    }

    @Test
    public void saveEvent_PersistsEventToEventsCollection() {
        Event event = new Event("Persisted Event");
        event.setEventId(EVENT_ID);

        Task<Void> mockSetTask = mock(Task.class);

        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
        when(mockEventDocumentRef.set(event)).thenReturn(mockSetTask);

        when(mockSetTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                            invocation.getArgument(0);
                    successListener.onSuccess(null);
                    return mockSetTask;
                });

        when(mockSetTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockSetTask);

        eventService.saveEvent(event);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).document(EVENT_ID);
        verify(mockEventDocumentRef, atLeastOnce()).set(event);
        verify(mockSetTask, atLeastOnce()).addOnSuccessListener(any());
    }
}
