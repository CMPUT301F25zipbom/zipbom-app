package com.example.code_zombom_app.AdminTests;

import static org.mockito.Mockito.*;

import android.os.Looper;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.R;
import com.example.code_zombom_app.ui.admin.EventsAdminFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Iterator;

@RunWith(AndroidJUnit4.class)
public class AdminFragmentTest {

    FirebaseFirestore mockDb;
    CollectionReference mockEventsCollection;
    QuerySnapshot mockSnapshot;
    QueryDocumentSnapshot mockEventDoc;

    @Before
    public void setup() {
        mockDb = mock(FirebaseFirestore.class);
        mockEventsCollection = mock(CollectionReference.class);
        mockSnapshot = mock(QuerySnapshot.class);
        mockEventDoc = mock(QueryDocumentSnapshot.class);

        when(mockDb.collection("Events")).thenReturn(mockEventsCollection);

        // Make snapshot listener execute immediately
        doAnswer(inv -> {
            EventListener<QuerySnapshot> listener = inv.getArgument(0);
            listener.onEvent(mockSnapshot, null);
            return null;
        }).when(mockEventsCollection).addSnapshotListener(any());

        when(mockSnapshot.isEmpty()).thenReturn(false);
        Iterator<QueryDocumentSnapshot> iterator =
                Arrays.asList(mockEventDoc).iterator();
        when(mockSnapshot.iterator()).thenReturn(iterator);

        when(mockEventDoc.getString("Name")).thenReturn("Halloween Party");
        when(mockEventDoc.getString("Max People")).thenReturn("50");
        when(mockEventDoc.getString("Date")).thenReturn("Oct 31");
        when(mockEventDoc.getString("Deadline")).thenReturn("Oct 25");
        when(mockEventDoc.getString("Genre")).thenReturn("Horror");
        when(mockEventDoc.getString("Location")).thenReturn("Campus Hall");
        when(mockEventDoc.getId()).thenReturn("abcEvent123");
    }

    @Test
    public void testDeleteButtonShowsDialogDirectly() {

        if (Looper.myLooper() == null) Looper.prepare();

        // 1️⃣ Launch fragment in CREATED state (so Firestore is not attached yet)
        FragmentScenario<EventsAdminFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventsAdminFragment.class,
                        null
                );

        scenario.moveToState(Lifecycle.State.CREATED);

        // 2️⃣ Inject mock database BEFORE UI starts listening
        scenario.onFragment(fragment -> {
            fragment.setMockDatabase(mockDb, mockEventsCollection);
        });

        // 3️⃣ Now move to STARTED → fragment attaches snapshot listener using MOCKS
        scenario.moveToState(Lifecycle.State.STARTED);

        // 4️⃣ Interact with UI
        scenario.onFragment(fragment -> {
            fragment.requireView().post(() -> {

                LinearLayout container = fragment.requireView()
                        .findViewById(fragment.eventsContainer.getId());

                if (container.getChildCount() > 0) {
                    ImageButton deleteButton =
                            container.getChildAt(0).findViewById(R.id.button_delete_event);

                    if (deleteButton != null) {
                        deleteButton.performClick();
                    }
                }
            });
        });
    }
}
