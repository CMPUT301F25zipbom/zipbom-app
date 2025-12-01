package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.ui.admin.EventsAdminFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminBrowseEventsTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    private EventsAdminFragment fragment;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fragment = new EventsAdminFragment();

        Field dbField = EventsAdminFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, mockFirestore);

        Field eventsField = EventsAdminFragment.class.getDeclaredField("eventsdb");
        eventsField.setAccessible(true);
        eventsField.set(fragment, mockEventsCollection);

        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            EventListener<QuerySnapshot> listener =
                    (EventListener<QuerySnapshot>) invocation.getArgument(0);
            // Do not invoke the listener to avoid UI dependencies.
            return null;
        }).when(mockEventsCollection).addSnapshotListener(any(EventListener.class));
    }

    @Test
    public void loadEventsFromDatabase_AdminBrowsesEvents_RegistersSnapshotListener() throws Exception {
        java.lang.reflect.Method method = EventsAdminFragment.class
                .getDeclaredMethod("loadEventsFromDatabase");
        method.setAccessible(true);
        method.invoke(fragment);

        verify(mockFirestore, atLeastOnce()).collection("Events");
        verify(mockEventsCollection, atLeastOnce()).addSnapshotListener(any(EventListener.class));
    }
}
