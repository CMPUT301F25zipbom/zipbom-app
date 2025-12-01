package com.example.code_zombom_app.Helpers.Models;

import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional coverage for entrant profile updates/deletion and notification preferences (US 01.02.02, US 01.02.04, US 01.04.03).
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadUploadProfileUpdateTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockProfilesCollection;

    @Mock
    private CollectionReference mockNotificationPrefsCollection;

    @Mock
    private DocumentReference mockOldProfileRef;

    @Mock
    private DocumentReference mockNewProfileRef;

    @Mock
    private DocumentReference mockOldPreferenceRef;

    @Mock
    private DocumentReference mockNewPreferenceRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    private LoadUploadProfileModel model;

    private static final String OLD_EMAIL = "old@example.com";
    private static final String NEW_EMAIL = "new@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        model = new LoadUploadProfileModel(mockFirestore);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockFirestore.collection("NotificationPreferences")).thenReturn(mockNotificationPrefsCollection);

        when(mockProfilesCollection.document(OLD_EMAIL)).thenReturn(mockOldProfileRef);
        when(mockProfilesCollection.document(NEW_EMAIL)).thenReturn(mockNewProfileRef);

           }


    @Test
    public void editProfile_EmailChanged_NewEmailAlreadyExists_DoesNotDeleteOldProfile() {
        Profile oldProfile = new Entrant("Old", OLD_EMAIL, "111-1111");
        Profile newProfile = new Entrant("New", NEW_EMAIL, "222-2222");

        Task<DocumentSnapshot> mockGetNewEmailTask = mock(Task.class);
        when(mockNewProfileRef.get()).thenReturn(mockGetNewEmailTask);
        when(mockDocumentSnapshot.exists()).thenReturn(true);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockDocumentSnapshot);
            return mockGetNewEmailTask;
        }).when(mockGetNewEmailTask).addOnSuccessListener(any());

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetNewEmailTask)
                .when(mockGetNewEmailTask).addOnFailureListener(any());

        model.editProfile(oldProfile, newProfile);

        verify(mockNewProfileRef, atLeastOnce()).get();
        verify(mockOldProfileRef, never()).delete();
        verify(mockNewProfileRef, never()).set(any(Profile.class));
    }

    @Test
    public void deleteProfile_RemovesProfileAndNotificationPreference() {
        DocumentReference profileRef = mock(DocumentReference.class);
        DocumentReference preferenceRef = mock(DocumentReference.class);

        when(mockProfilesCollection.document("delete@example.com")).thenReturn(profileRef);
        when(mockNotificationPrefsCollection.document("delete@example.com")).thenReturn(preferenceRef);

        Task<Void> mockDeleteProfileTask = mock(Task.class);
        when(profileRef.delete()).thenReturn(mockDeleteProfileTask);

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockDeleteProfileTask;
        }).when(mockDeleteProfileTask).addOnSuccessListener(any());

        doAnswer((Answer<Task<Void>>) invocation -> mockDeleteProfileTask)
                .when(mockDeleteProfileTask).addOnFailureListener(any());

        Task<Void> mockDeletePreferenceTask = mock(Task.class);
        when(preferenceRef.delete()).thenReturn(mockDeletePreferenceTask);

        model.deleteProfile("delete@example.com");

        verify(profileRef, atLeastOnce()).delete();
        verify(preferenceRef, atLeastOnce()).delete();
    }

    @Test
    public void deleteProfile_FirestoreFailure_DoesNotTouchNotificationPreferences() {
        DocumentReference profileRef = mock(DocumentReference.class);
        when(mockProfilesCollection.document(OLD_EMAIL)).thenReturn(profileRef);

        Task<Void> mockDeleteTask = mock(Task.class);
        when(profileRef.delete()).thenReturn(mockDeleteTask);

        doAnswer((Answer<Task<Void>>) invocation -> mockDeleteTask)
                .when(mockDeleteTask).addOnSuccessListener(any());

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(new Exception("delete failed"));
            return mockDeleteTask;
        }).when(mockDeleteTask).addOnFailureListener(any());

        model.deleteProfile(OLD_EMAIL);

        verify(profileRef, atLeastOnce()).delete();
        verify(mockNotificationPrefsCollection, never()).document(any(String.class));
    }
}


