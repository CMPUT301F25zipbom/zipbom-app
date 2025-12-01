package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminProfileManagementTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockProfilesCollection;

    @Mock
    private CollectionReference mockNotificationPrefsCollection;

    private LoadUploadProfileModel profileModel;

    private static final String PROFILE_EMAIL = "user.to.delete@example.com";
    private static final String ORGANIZER_EMAIL = "organizer.to.delete@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        profileModel = new LoadUploadProfileModel(mockFirestore);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockFirestore.collection("NotificationPreferences")).thenReturn(mockNotificationPrefsCollection);
    }

    // ------------------------------------------------------------
    // BASIC TEST TO CONFIRM DISCOVERY (Do not delete)
    // ------------------------------------------------------------
    @Test
    public void testDiscovery() {
        // This ensures JUnit detects this test class.
        assert(true);
    }

    @Test
    public void deleteProfile_AdminRemovesProfile_DeletesProfileAndNotificationPreference() {
        DocumentReference profileRef = mock(DocumentReference.class);
        DocumentReference notificationRef = mock(DocumentReference.class);

        when(mockProfilesCollection.document(PROFILE_EMAIL)).thenReturn(profileRef);
        when(mockNotificationPrefsCollection.document(PROFILE_EMAIL)).thenReturn(notificationRef);

        Task<Void> mockDeleteProfileTask = mock(Task.class);
        when(profileRef.delete()).thenReturn(mockDeleteProfileTask);

        when(mockDeleteProfileTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<Void> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(null);
                    return mockDeleteProfileTask;
                });

        when(mockDeleteProfileTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockDeleteProfileTask);

        Task<Void> mockDeleteNotificationTask = mock(Task.class);
        when(notificationRef.delete()).thenReturn(mockDeleteNotificationTask);

        profileModel.deleteProfile(PROFILE_EMAIL);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(PROFILE_EMAIL);
        verify(profileRef, atLeastOnce()).delete();

        verify(mockFirestore, atLeastOnce()).collection("NotificationPreferences");
        verify(mockNotificationPrefsCollection, atLeastOnce()).document(PROFILE_EMAIL);
        verify(notificationRef, atLeastOnce()).delete();
    }

    @Test
    public void deleteProfile_AdminRemovesProfile_FirestoreFailure_DoesNotDeleteNotificationPreference() {
        DocumentReference profileRef = mock(DocumentReference.class);
        when(mockProfilesCollection.document(PROFILE_EMAIL)).thenReturn(profileRef);

        Task<Void> mockDeleteTask = mock(Task.class);
        when(profileRef.delete()).thenReturn(mockDeleteTask);

        when(mockDeleteTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockDeleteTask);

        when(mockDeleteTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnFailureListener listener =
                            invocation.getArgument(0);
                    listener.onFailure(new Exception("Delete failed"));
                    return mockDeleteTask;
                });

        profileModel.deleteProfile(PROFILE_EMAIL);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(profileRef, atLeastOnce()).delete();

        // Notification prefs should NOT be touched
        verify(mockFirestore, never()).collection("NotificationPreferences");
        verify(mockNotificationPrefsCollection, never()).document(any());
    }

    @Test
    public void deleteProfile_AdminRemovesOrganizerProfile_UsesProfilesCollection() {
        DocumentReference organizerRef = mock(DocumentReference.class);
        when(mockProfilesCollection.document(ORGANIZER_EMAIL)).thenReturn(organizerRef);

        Task<Void> mockDeleteTask = mock(Task.class);
        when(organizerRef.delete()).thenReturn(mockDeleteTask);

        when(mockDeleteTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<Void> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(null);
                    return mockDeleteTask;
                });

        when(mockDeleteTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<Void>>) invocation -> mockDeleteTask);

        profileModel.deleteProfile(ORGANIZER_EMAIL);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(ORGANIZER_EMAIL);
        verify(organizerRef, atLeastOnce()).delete();
    }
}
