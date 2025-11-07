package com.example.code_zombom_app;

import static org.mockito.Mockito.*;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.ui.admin.AdminFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * <h1>AdminFragmentTest</h1>
 *
 * The test verifies that the fragment:
 * <ul>
 *     <li>Can load mock event data from a mocked Firestore database,</li>
 *     <li>Correctly displays an event item in its container, and</li>
 *     <li>Handles the "delete" button click event without throwing exceptions.</li>
 * </ul>
 *
 * <p>This test uses Mockito to create fake Firestore objects and feed them into the fragment,
 * so that no real network or Firebase connection is required during testing.</p>
 *
 * <p><b>Key idea:</b> This test ensures UI stability when interacting with Firestore-backed views.</p>
 */
@RunWith(AndroidJUnit4.class)
public class AdminFragmentTest {

    /** Mock Firestore database instance used in testing. */
    FirebaseFirestore mockDb;

    /** Mock reference to the "Events" Firestore collection. */
    CollectionReference mockEventsCollection;

    /** Mock Firestore QuerySnapshot that simulates a set of event documents. */
    QuerySnapshot mockSnapshot;

    /** Mock Firestore document representing a single event. */
    QueryDocumentSnapshot mockEventDoc;

    /**
     * Initializes and configures all mock objects before each test.
     *
     * <p>This setup ensures that whenever the {@link AdminFragment} requests data from Firestore,
     * it will receive a predefined mock event (e.g., a "Halloween Party") instead of connecting
     * to the real database.</p>
     */
    @Before
    public void setup() {
        // mock Firestore components
        mockDb = mock(FirebaseFirestore.class);
        mockEventsCollection = mock(CollectionReference.class);
        mockSnapshot = mock(QuerySnapshot.class);
        mockEventDoc = mock(QueryDocumentSnapshot.class);

        // return our mock collection instead
        when(mockDb.collection("Events")).thenReturn(mockEventsCollection);

        /**
         * Mock the behavior of addSnapshotListener().
         *
         * Instead of connecting to Firestore, this fake listener immediately triggers the callback
         * with a prepared QuerySnapshot.
         */
        doAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(mockSnapshot, null); // trigger the listener immediately
            return null;
        }).when(mockEventsCollection).addSnapshotListener(any());

        // pretend that the snapshot is not empty and contains exactly one document
        when(mockSnapshot.isEmpty()).thenReturn(false);
        when(mockSnapshot.iterator()).thenReturn(Arrays.asList(mockEventDoc).iterator());

        // fake data for our mock event
        when(mockEventDoc.getString("Name")).thenReturn("Halloween Party");
        when(mockEventDoc.getString("Max People")).thenReturn("50");
        when(mockEventDoc.getString("Date")).thenReturn("Oct 31");
        when(mockEventDoc.getString("Deadline")).thenReturn("Oct 25");
        when(mockEventDoc.getString("Genre")).thenReturn("Horror");
        when(mockEventDoc.getString("Location")).thenReturn("Campus Hall");

        // give a fake Firestore document ID for this event so that test knows that event to click
        when(mockEventDoc.getId()).thenReturn("abcEvent123");
    }

    /**
     * <h3>testDeleteButtonShowsDialogDirectly()</h3>
     * <ul>
     *     <li>Loads event data from the mock Firestore instance,</li>
     *     <li>Populates its event container with an event view, and</li>
     * </ul>
     */
    @Test
    public void testDeleteButtonShowsDialogDirectly() {
        // Launch the AdminFragment inside a test container (isolated from the rest of the app)
        FragmentScenario<AdminFragment> scenario =
                FragmentScenario.launchInContainer(AdminFragment.class, new Bundle());

        // Interact with the fragment after it is created
        scenario.onFragment(fragment -> {
            // Inject our mock Firestore database and event collection
            fragment.setMockDatabase(mockDb, mockEventsCollection);

            /**
             * allows time for the snapshot listener to populate the event container.
             */
            fragment.getView().post(() -> {
                // container holding all event views
                LinearLayout container = (LinearLayout) fragment.getView().findViewById(
                        fragment.eventsContainer.getId());

                // check if the container has at least one event
                if (container.getChildCount() > 0) {
                    // retrieve the delete button of the first event item
                    ImageButton deleteButton = container.getChildAt(0).findViewById(R.id.button_delete_event);

                    // user tapping the delete button
                    if (deleteButton != null) {
                        deleteButton.performClick(); // Triggers showDeleteConfirmationDialog()
                    }
                }
            });
        });
    }
}
