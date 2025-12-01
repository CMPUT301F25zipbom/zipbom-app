package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.Helpers.Event.EventService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminEventManagementTest {
    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private CollectionReference mockEventsCollection;
    @Mock
    private DocumentReference mockEventDocumentRef;

    private EventService eventService;
    private static final String EVENT_ID = "admin-event-123";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        eventService = new EventService(mockFirestore);
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
    }

    @Test
    public void deleteEvent_AdminRemovesEvent_SuccessfulDeletion() {
        Task<Void> mockDeleteTask = mock(Task.class);
        when(mockEventDocumentRef.delete()).thenReturn(mockDeleteTask);

        when(mockDeleteTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                            invocation.getArgument(0);
                    successListener.onSuccess(null);
                    return mockDeleteTask;
                });

        when(mockDeleteTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockDeleteTask);

        eventService.deleteEvent(EVENT_ID);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).document(EVENT_ID);
        verify(mockEventDocumentRef, atLeastOnce()).delete();
        verify(mockDeleteTask, atLeastOnce()).addOnSuccessListener(any());
    }

    @Test
    public void deleteEvent_AdminRemovesEvent_FirestoreFailure() {
        Task<Void> mockDeleteTask = mock(Task.class);
        when(mockEventDocumentRef.delete()).thenReturn(mockDeleteTask);

        when(mockDeleteTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockDeleteTask);

        when(mockDeleteTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnFailureListener failureListener =
                            invocation.getArgument(0);
                    failureListener.onFailure(new Exception("Delete failed"));
                    return mockDeleteTask;
                });

        eventService.deleteEvent(EVENT_ID);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).document(EVENT_ID);
        verify(mockEventDocumentRef, atLeastOnce()).delete();
        verify(mockDeleteTask, atLeastOnce()).addOnFailureListener(any());
    }
}
