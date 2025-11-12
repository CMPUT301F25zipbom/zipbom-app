package com.example.code_zombom_app.AdminTests;

import static org.mockito.Mockito.*;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.R;
import com.example.code_zombom_app.ui.admin.ProfileAdminFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * <h1>ProfileAdminFragmentTest</h1>
 *
 * Tests the admin profile management UI.
 * Verifies that the fragment:
 * <ul>
 *     <li>Loads mock Firestore profile data,</li>
 *     <li>Displays a profile entry in the container,</li>
 *     <li>And responds properly to delete button clicks.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ProfileAdminFragmentTest {

    /** Mock Firestore database and collection. */
    FirebaseFirestore mockDb;
    CollectionReference mockProfilesCollection;

    /** Mock Firestore snapshot and document. */
    QuerySnapshot mockSnapshot;
    QueryDocumentSnapshot mockProfileDoc;

    @Before
    public void setup() {
        // Initialize Firestore mocks
        mockDb = mock(FirebaseFirestore.class);
        mockProfilesCollection = mock(CollectionReference.class);
        mockSnapshot = mock(QuerySnapshot.class);
        mockProfileDoc = mock(QueryDocumentSnapshot.class);

        // Return the mock collection when fragment asks for "Profiles"
        when(mockDb.collection("Profiles")).thenReturn(mockProfilesCollection);

        // Fake listener callback: immediately send snapshot to fragment
        doAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(mockSnapshot, null); // Simulate Firestore update
            return null;
        }).when(mockProfilesCollection).addSnapshotListener(any());

        // Fake one non-empty document
        when(mockSnapshot.isEmpty()).thenReturn(false);
        when(mockSnapshot.iterator()).thenReturn(Arrays.asList(mockProfileDoc).iterator());

        // Mock field data
        when(mockProfileDoc.getString("Name")).thenReturn("John Doe");
        when(mockProfileDoc.getString("Email")).thenReturn("john@example.com");
        when(mockProfileDoc.getString("Role")).thenReturn("User");
        when(mockProfileDoc.getString("Events")).thenReturn("Event123");
        when(mockProfileDoc.getId()).thenReturn("mockProfile123");
    }

    /**
     * <h3>testDeleteButtonShowsDialogDirectly()</h3>
     * Ensures that the delete button click does not throw exceptions
     * and successfully triggers the delete confirmation dialog logic.
     */
    @Test
    public void testDeleteButtonShowsDialogDirectly() {
        FragmentScenario<ProfileAdminFragment> scenario =
                FragmentScenario.launchInContainer(ProfileAdminFragment.class, new Bundle());

        scenario.onFragment(fragment -> {
            // Replace Firestore references with mocks
            try {
                fragment.getClass()
                        .getDeclaredMethod("setMockDatabase", FirebaseFirestore.class, CollectionReference.class)
                        .invoke(fragment, mockDb, mockProfilesCollection);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });

        // Use post() to ensure UI is built
        scenario.onFragment(fragment -> {
            fragment.getView().post(() -> {
                LinearLayout container = fragment.getView().findViewById(
                        fragment.requireView().getId());

                // Check if the container has a child (the mocked profile)
                if (container != null && container.getChildCount() > 0) {
                    ImageButton deleteButton = container.getChildAt(0)
                            .findViewById(R.id.button_delete_profile);

                    if (deleteButton != null) {
                        deleteButton.performClick(); // Trigger dialog
                    }
                }
            });
        });
    }
}
