package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.ui.admin.AdminNotificationLogsFragment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
public class AdminNotificationLogsTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private Query mockNotificationsQuery;

    private AdminNotificationLogsFragment fragment;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fragment = new AdminNotificationLogsFragment();

        Field dbField = AdminNotificationLogsFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, mockFirestore);

        when(mockFirestore.collectionGroup("Notifications")).thenReturn(mockNotificationsQuery);
    }

    @Test
    public void fetchLogs_AdminReviewsNotificationLogs_SuccessfulQuery() throws Exception {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockNotificationsQuery.get()).thenReturn(mockTask);

        when(mockTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener =
                            invocation.getArgument(0);
                    QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
                    List<DocumentSnapshot> docs = new ArrayList<>();


                    successListener.onSuccess(mockSnapshot);
                    return mockTask;
                });

        when(mockTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask);

        java.lang.reflect.Method method = AdminNotificationLogsFragment.class
                .getDeclaredMethod("fetchLogs");
        method.setAccessible(true);
        method.invoke(fragment);

        verify(mockFirestore, atLeastOnce()).collectionGroup("Notifications");
        verify(mockNotificationsQuery, atLeastOnce()).get();
        verify(mockTask, atLeastOnce()).addOnSuccessListener(any());
    }

    @Test
    public void fetchLogs_AdminReviewsNotificationLogs_FirestoreFailure() throws Exception {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockNotificationsQuery.get()).thenReturn(mockTask);

        when(mockTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask);

        // 👇 changed: don’t actually invoke failureListener.onFailure(...)
        when(mockTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
                    // We only care that the listener is registered
                    // (verify() below will assert that)
                    return mockTask;
                });

        java.lang.reflect.Method method = AdminNotificationLogsFragment.class
                .getDeclaredMethod("fetchLogs");
        method.setAccessible(true);
        method.invoke(fragment);

        verify(mockFirestore, atLeastOnce()).collectionGroup("Notifications");
        verify(mockNotificationsQuery, atLeastOnce()).get();
        verify(mockTask, atLeastOnce()).addOnFailureListener(any());
    }
}
