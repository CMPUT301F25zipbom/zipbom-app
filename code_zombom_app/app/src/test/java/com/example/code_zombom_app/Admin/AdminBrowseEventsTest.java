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
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that EventsAdminFragment registers a snapshot listener on the "Events"
 * collection when loading events for the admin to browse.
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminBrowseEventsTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockEventsCollection;

    private EventsAdminFragment fragment;

    @Before
    public void setUp() throws Exception {
        // Create fragment instance (no need to attach to an Activity/FragmentManager)
        fragment = new EventsAdminFragment();

        // Inject mocked Firestore instance into private field 'db'
        Field dbField = EventsAdminFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, mockFirestore);

        // Inject mocked Events collection into private field 'eventsdb'
        Field eventsField = EventsAdminFragment.class.getDeclaredField("eventsdb");
        eventsField.setAccessible(true);
        eventsField.set(fragment, mockEventsCollection);

        // When the fragment asks Firestore for the "Events" collection, return our mock
        when(mockFirestore.collection("Events")).thenReturn(mockEventsCollection);

        // Stub addSnapshotListener so it accepts any listener but does nothing
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            EventListener<QuerySnapshot> listener =
                    (EventListener<QuerySnapshot>) invocation.getArgument(0);
            // We intentionally do NOT invoke the listener to avoid UI / LiveData logic.
            return null;
        }).when(mockEventsCollection).addSnapshotListener(any(EventListener.class));
    }

    @Test
    public void loadEventsFromDatabase_AdminBrowsesEvents_RegistersSnapshotListener() throws Exception {
        // Call the private loadEventsFromDatabase() method via reflection
        Method method = EventsAdminFragment.class.getDeclaredMethod("loadEventsFromDatabase");
        method.setAccessible(true);
        method.invoke(fragment);

        // Verify Firestore "Events" collection was requested
        verify(mockFirestore, atLeastOnce()).collection("Events");

        // Verify a snapshot listener was registered on that collection
        verify(mockEventsCollection, atLeastOnce()).addSnapshotListener(any(EventListener.class));
    }
}
