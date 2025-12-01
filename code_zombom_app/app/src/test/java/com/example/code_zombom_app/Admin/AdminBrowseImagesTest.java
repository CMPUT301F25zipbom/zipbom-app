package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.ui.admin.PostersAdminFragment;
import com.example.code_zombom_app.ui.admin.PostersAdapter;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminBrowseImagesTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    private PostersAdminFragment fragment;
    private List<com.google.firebase.firestore.DocumentSnapshot> posterEvents;
    private PostersAdapter mockAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fragment = new PostersAdminFragment();

        Field dbField = PostersAdminFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, mockFirestore);

        posterEvents = new ArrayList<>();
        mockAdapter = mock(PostersAdapter.class);

        Field postersField = PostersAdminFragment.class.getDeclaredField("posterEvents");
        postersField.setAccessible(true);
        postersField.set(fragment, posterEvents);

        Field adapterField = PostersAdminFragment.class.getDeclaredField("adapter");
        adapterField.setAccessible(true);
        adapterField.set(fragment, mockAdapter);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);
    }

    @Test
    public void fetchPosters_AdminBrowsesImages_QueriesEventsCollection() throws Exception {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);

        when(mockTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener =
                            invocation.getArgument(0);
                    QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
                    successListener.onSuccess(mockSnapshot);
                    return mockTask;
                });

        when(mockTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask);

        java.lang.reflect.Method method = PostersAdminFragment.class
                .getDeclaredMethod("fetchPosters");
        method.setAccessible(true);
        method.invoke(fragment);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).get();
        verify(mockTask, atLeastOnce()).addOnSuccessListener(any());
    }

    @Test
    public void fetchPosters_FirestoreFailure_AttachesFailureListener() throws Exception {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockEventsCollection.get()).thenReturn(mockTask);

        when(mockTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask);

        when(mockTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
                    com.google.android.gms.tasks.OnFailureListener failureListener =
                            invocation.getArgument(0);
                    failureListener.onFailure(new Exception("Query failed"));
                    return mockTask;
                });

        java.lang.reflect.Method method = PostersAdminFragment.class
                .getDeclaredMethod("fetchPosters");
        method.setAccessible(true);
        method.invoke(fragment);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).get();
        verify(mockTask, atLeastOnce()).addOnFailureListener(any());
    }
}
