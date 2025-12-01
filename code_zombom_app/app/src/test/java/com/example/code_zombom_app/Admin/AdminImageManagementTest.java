package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.ui.admin.PostersAdminFragment;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminImageManagementTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private DocumentReference mockEventDocumentRef;

    private PostersAdminFragment fragment;

    private static final String EVENT_ID = "poster-event-1";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fragment = new PostersAdminFragment();

        Field dbField = PostersAdminFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, mockFirestore);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(EVENT_ID)).thenReturn(mockEventDocumentRef);
    }

    @Test
    public void removePosterReferenceFromFirestore_Success_UpdatesPosterUrlToNull() throws Exception {
        Task<Void> mockUpdateTask = mock(Task.class);
        when(mockEventDocumentRef.update("posterUrl", null)).thenReturn(mockUpdateTask);

        when(mockUpdateTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                            invocation.getArgument(0);
                    successListener.onSuccess(null);
                    return mockUpdateTask;
                });

        when(mockUpdateTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockUpdateTask);

        Method method = PostersAdminFragment.class
                .getDeclaredMethod("removePosterReferenceFromFirestore", String.class);
        method.setAccessible(true);
        method.invoke(fragment, EVENT_ID);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).document(EVENT_ID);
        verify(mockEventDocumentRef, atLeastOnce()).update("posterUrl", null);
        verify(mockUpdateTask, atLeastOnce()).addOnSuccessListener(any());
    }

    @Test
    public void removePosterReferenceFromFirestore_Failure_AttachesFailureListener() throws Exception {
        Task<Void> mockUpdateTask = mock(Task.class);
        when(mockEventDocumentRef.update("posterUrl", null)).thenReturn(mockUpdateTask);

        when(mockUpdateTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockUpdateTask);

        when(mockUpdateTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnFailureListener failureListener =
                            invocation.getArgument(0);
                    failureListener.onFailure(new Exception("Update failed"));
                    return mockUpdateTask;
                });

        Method method = PostersAdminFragment.class
                .getDeclaredMethod("removePosterReferenceFromFirestore", String.class);
        method.setAccessible(true);
        method.invoke(fragment, EVENT_ID);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).document(EVENT_ID);
        verify(mockEventDocumentRef, atLeastOnce()).update("posterUrl", null);
        verify(mockUpdateTask, atLeastOnce()).addOnFailureListener(any());
    }
}
