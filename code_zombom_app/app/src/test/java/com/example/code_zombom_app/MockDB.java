package com.example.code_zombom_app;

import static org.mockito.Mockito.*;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fake Firestore-like database for testing Admin fragments.
 * Can be reused across multiple tests.
 */
public class MockDB {

    /** Map of eventId -> event data */
    private final Map<String, Map<String, Object>> events = new HashMap<>();

    /** Firestore mock representing the "events" collection */
    private final CollectionReference mockEventsCollection;

    /** Last registered snapshot listener */
    private EventListener<QuerySnapshot> eventListener;

    public MockDB() {
        mockEventsCollection = mock(CollectionReference.class);

        // Trigger listener when addSnapshotListener is called
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            EventListener<QuerySnapshot> listener =
                    (EventListener<QuerySnapshot>) invocation.getArgument(0);

            this.eventListener = listener;

            // Immediately fire the listener with current data
            listener.onEvent(makeSnapshot(), null);
            return null;
        }).when(mockEventsCollection)
                .addSnapshotListener(ArgumentMatchers.<EventListener<QuerySnapshot>>any());
    }

    /** Expose the mocked collection so tests can inject it where needed */
    public CollectionReference getMockEventsCollection() {
        return mockEventsCollection;
    }

    /** Add a fake event to the database */
    public void addEvent(String id, Map<String, Object> data) {
        events.put(id, new HashMap<>(data)); // store a copy to avoid accidental external mutation
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

    /** Build a QuerySnapshot-like object for current events */
    private QuerySnapshot makeSnapshot() {
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        List<QueryDocumentSnapshot> docs = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : events.entrySet()) {
            String docId = entry.getKey();
            Map<String, Object> data = entry.getValue();

            QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

            when(doc.getId()).thenReturn(docId);
            when(doc.getData()).thenReturn(data);

            // getString("fieldName") -> data.get("fieldName").toString()
            when(doc.getString(ArgumentMatchers.anyString()))
                    .thenAnswer(invocation -> {
                        String key = invocation.getArgument(0, String.class);
                        Object val = data.get(key);
                        return val != null ? val.toString() : null;
                    });

            docs.add(doc);
        }

        when(snapshot.isEmpty()).thenReturn(docs.isEmpty());
        when(snapshot.iterator()).thenReturn(docs.iterator());

        return snapshot;
    }
}
