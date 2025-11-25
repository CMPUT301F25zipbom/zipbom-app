package com.example.code_zombom_app;

import static org.mockito.Mockito.*;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A fake Firestore-like database for testing Admin fragments.
 * Can be reused across multiple tests.
 */
public class mockDB {

    /** Map of eventId -> event data */
    public HashMap<String, Map<String, Object>> events = new HashMap<>();

    /** Firestore mocks */
    public CollectionReference mockEventsCollection;
    public EventListener<QuerySnapshot> eventListener;

    public mockDB() {
        mockEventsCollection = mock(CollectionReference.class);

        // Trigger listener when addSnapshotListener is called
        doAnswer(invocation -> {
            eventListener = invocation.getArgument(0);
            // immediately fire the listener
            eventListener.onEvent(makeSnapshot(), null);
            return null;
        }).when(mockEventsCollection).addSnapshotListener(any());
    }

    /** Add a fake event to the database */
    public void addEvent(String id, Map<String, Object> data) {
        events.put(id, data);
        // notify listener if exists
        if (eventListener != null) {
            eventListener.onEvent(makeSnapshot(), null);
        }
    }

    /** Remove an event */
    public void removeEvent(String id) {
        events.remove(id);
        if (eventListener != null) {
            eventListener.onEvent(makeSnapshot(), null);
        }
    }

    /** Build a QuerySnapshot-like iterator for events */
    private QuerySnapshot makeSnapshot() {
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        ArrayList<QueryDocumentSnapshot> list = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : events.entrySet()) {
            QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
            when(doc.getData()).thenReturn(entry.getValue());
            when(doc.getString(any())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Object val = entry.getValue().get(key);
                return val != null ? val.toString() : null;
            });
            when(doc.getId()).thenReturn(entry.getKey());
            list.add(doc);
        }

        when(snapshot.isEmpty()).thenReturn(list.isEmpty());
        when(snapshot.iterator()).thenReturn(list.iterator());

        return snapshot;
    }
}
