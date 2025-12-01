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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LoadUploadProfileModel.editProfile() and deleteProfile().
 * Covers updating and deleting entrant profiles, as well as notification preference syncing.
 *
 * User stories:
 *  - US 01.02.02 (update profile information)
 *  - US 01.02.04 (delete profile)
 *  - US 01.04.03 (opt out of notifications – persisted via notification preferences)
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadUploadProfileModelEditProfileTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockProfilesCollection;

    @Mock
    private CollectionReference mockNotificationPrefsCollection;

    @Mock
    private DocumentReference mockOldProfileDocumentRef;

    @Mock
    private DocumentReference mockNewProfileDocumentRef;

    @Mock
    private DocumentReference mockNotificationPrefOldRef;

    @Mock
    private DocumentReference mockNotificationPrefNewRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    private LoadUploadProfileModel loadUploadProfileModel;

    private static final String OLD_EMAIL = "old.email@example.com";
    private static final String NEW_EMAIL = "new.email@example.com";
    private static final String NORMALIZED_OLD = "old.email@example.com";
    private static final String NORMALIZED_NEW = "new.email@example.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        loadUploadProfileModel = new LoadUploadProfileModel(mockFirestore);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockFirestore.collection("NotificationPreferences")).thenReturn(mockNotificationPrefsCollection);

        when(mockProfilesCollection.document(OLD_EMAIL)).thenReturn(mockOldProfileDocumentRef);
        when(mockProfilesCollection.document(NEW_EMAIL)).thenReturn(mockNewProfileDocumentRef);

        when(mockNotificationPrefsCollection.document(NORMALIZED_OLD)).thenReturn(mockNotificationPrefOldRef);
        when(mockNotificationPrefsCollection.document(NORMALIZED_NEW)).thenReturn(mockNotificationPrefNewRef);
    }

    /**
     * When the email stays the same, editProfile should update the existing document
     * and sync notification preferences for the entrant.
     */
    @Test
    public void editProfile_SameEmail_UpdatesExistingProfileAndSyncsNotificationPreference() {
        Entrant oldProfile = new Entrant("Old Name", OLD_EMAIL, "111-111-1111");
        Entrant newProfile = new Entrant("New Name", OLD_EMAIL, "222-222-2222");
        newProfile.setNotificationsEnabled(false); // opt-out should be persisted

        // --- Firestore get() on existing profile ---
        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockOldProfileDocumentRef.get()).thenReturn(mockGetTask);

        when(mockDocumentSnapshot.exists()).thenReturn(true);
        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // --- Firestore set() on existing profile ---
        Task<Void> mockSetTask = mock(Task.class);
        when(mockOldProfileDocumentRef.set(eq(newProfile))).thenReturn(mockSetTask);

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null); // simulate successful profile update
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // --- NotificationPreferences.set(...) stub for SAME email ---
        Task<Void> mockNotificationSetTask = mock(Task.class);
        when(mockNotificationPrefOldRef.set(any(Map.class))).thenReturn(mockNotificationSetTask);

        // Act
        loadUploadProfileModel.editProfile(oldProfile, newProfile);

        // Assert - Firestore profile path and update
        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(OLD_EMAIL);
        verify(mockOldProfileDocumentRef, atLeastOnce()).get();
        verify(mockOldProfileDocumentRef, atLeastOnce()).set(eq(newProfile));

        // Assert - notification preferences synced using normalized OLD email key
        verify(mockFirestore, atLeastOnce()).collection("NotificationPreferences");
        verify(mockNotificationPrefsCollection, atLeastOnce()).document(NORMALIZED_OLD);

        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockNotificationPrefOldRef, atLeastOnce()).set(payloadCaptor.capture());

        Map payload = payloadCaptor.getValue();
        assertNotNull("Notification preference payload should not be null", payload);
        assertEquals("Email should be trimmed/lowercased",
                NORMALIZED_OLD, ((String) payload.get("email")).toLowerCase());
        assertEquals("notificationEnabled should reflect entrant preference",
                Boolean.FALSE, payload.get("notificationEnabled"));
    }

    /**
     * When the email changes and the new email is free, editProfile should:
     *  - verify the new email does not already exist
     *  - delete the old profile document and its notification preference
     *  - create the profile under the new email and sync notification preferences.
     */
    @Test
    public void editProfile_EmailChanged_NewEmailNotTaken_MovesProfileAndSyncsPreferences() {
        Entrant oldProfile = new Entrant("Old Name", OLD_EMAIL, "111-111-1111");
        Entrant newProfile = new Entrant("New Name", NEW_EMAIL, "222-222-2222");
        newProfile.setNotificationsEnabled(true);

        // Step 1: check new email
        Task<DocumentSnapshot> mockCheckNewEmailTask = mock(Task.class);
        when(mockNewProfileDocumentRef.get()).thenReturn(mockCheckNewEmailTask);

        when(mockDocumentSnapshot.exists()).thenReturn(false);
        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockCheckNewEmailTask;
        }).when(mockCheckNewEmailTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockCheckNewEmailTask)
                .when(mockCheckNewEmailTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Step 2: delete old profile
        Task<Void> mockDeleteOldTask = mock(Task.class);
        when(mockOldProfileDocumentRef.delete()).thenReturn(mockDeleteOldTask);

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockDeleteOldTask;
        }).when(mockDeleteOldTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockDeleteOldTask)
                .when(mockDeleteOldTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // deleteNotificationPreference(oldEmail)
        Task<Void> mockDeleteNotificationTask = mock(Task.class);
        when(mockNotificationPrefOldRef.delete()).thenReturn(mockDeleteNotificationTask);

        // Step 3: set new profile and syncNotificationPreference(newProfile)
        Task<Void> mockSetNewTask = mock(Task.class);
        when(mockNewProfileDocumentRef.set(eq(newProfile))).thenReturn(mockSetNewTask);

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetNewTask;
        }).when(mockSetNewTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetNewTask)
                .when(mockSetNewTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        Task<Void> mockNotificationSetTask = mock(Task.class);
        when(mockNotificationPrefNewRef.set(any(Map.class))).thenReturn(mockNotificationSetTask);

        // Act
        loadUploadProfileModel.editProfile(oldProfile, newProfile);

        // Assert - new email checked first
        verify(mockProfilesCollection, atLeastOnce()).document(NEW_EMAIL);
        verify(mockNewProfileDocumentRef, atLeastOnce()).get();

        // Old profile deleted and its notification preference removed
        verify(mockProfilesCollection, atLeastOnce()).document(OLD_EMAIL);
        verify(mockOldProfileDocumentRef, atLeastOnce()).delete();
        verify(mockNotificationPrefsCollection, atLeastOnce()).document(NORMALIZED_OLD);
        verify(mockNotificationPrefOldRef, atLeastOnce()).delete();

        // New profile written under the new email
        verify(mockNewProfileDocumentRef, atLeastOnce()).set(eq(newProfile));

        // Notification preference synced for the new email
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockNotificationPrefNewRef, atLeastOnce()).set(payloadCaptor.capture());

        Map payload = payloadCaptor.getValue();
        assertNotNull("Notification preference payload should not be null", payload);
        assertEquals("Email should be trimmed/lowercased",
                NORMALIZED_NEW, ((String) payload.get("email")).toLowerCase());
        assertEquals("notificationEnabled should reflect entrant preference",
                Boolean.TRUE, payload.get("notificationEnabled"));
    }

    /**
     * If the new email already exists, editProfile should fail gracefully and
     * NOT delete the old profile document or write the new one.
     */
    @Test
    public void editProfile_EmailChanged_NewEmailAlreadyExists_DoesNotModifyProfiles() {
        Profile oldProfile = new Entrant("Old Name", OLD_EMAIL, "111-111-1111");
        Profile newProfile = new Entrant("New Name", NEW_EMAIL, "222-222-2222");

        Task<DocumentSnapshot> mockCheckNewEmailTask = mock(Task.class);
        when(mockNewProfileDocumentRef.get()).thenReturn(mockCheckNewEmailTask);

        when(mockDocumentSnapshot.exists()).thenReturn(true); // new email already taken
        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockCheckNewEmailTask;
        }).when(mockCheckNewEmailTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockCheckNewEmailTask)
                .when(mockCheckNewEmailTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        loadUploadProfileModel.editProfile(oldProfile, newProfile);

        // Assert - new email was checked
        verify(mockProfilesCollection, atLeastOnce()).document(NEW_EMAIL);
        verify(mockNewProfileDocumentRef, atLeastOnce()).get();

        // Old profile is NOT deleted and new profile is NOT written
        verify(mockOldProfileDocumentRef, never()).delete();
        verify(mockNewProfileDocumentRef, never()).set(any(Profile.class));

        // Notification preferences are not touched for either email
        verify(mockNotificationPrefOldRef, never()).delete();
        verify(mockNotificationPrefNewRef, never()).set(any(Map.class));
    }

    /**
     * deleteProfile should remove the profile document and its notification preference
     * using Profiles/{email} and NotificationPreferences/{normalizedEmail}.
     */
    @Test
    public void deleteProfile_Success_DeletesProfileAndNotificationPreference() {
        String email = "user.to.delete@example.com";
        String normalized = "user.to.delete@example.com";

        DocumentReference profileRef = mock(DocumentReference.class);
        DocumentReference notificationRef = mock(DocumentReference.class);
        when(mockProfilesCollection.document(email)).thenReturn(profileRef);
        when(mockNotificationPrefsCollection.document(normalized)).thenReturn(notificationRef);

        Task<Void> mockDeleteProfileTask = mock(Task.class);
        when(profileRef.delete()).thenReturn(mockDeleteProfileTask);

        // Successful delete triggers deleteNotificationPreference(email)
        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockDeleteProfileTask;
        }).when(mockDeleteProfileTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockDeleteProfileTask)
                .when(mockDeleteProfileTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        Task<Void> mockDeleteNotificationTask = mock(Task.class);
        when(notificationRef.delete()).thenReturn(mockDeleteNotificationTask);

        // Act
        loadUploadProfileModel.deleteProfile(email);

        // Assert - profile deleted
        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(email);
        verify(profileRef, atLeastOnce()).delete();

        // Notification preference deleted using normalized key
        verify(mockFirestore, atLeastOnce()).collection("NotificationPreferences");
        verify(mockNotificationPrefsCollection, atLeastOnce()).document(normalized);
        verify(notificationRef, atLeastOnce()).delete();
    }

    /**
     * When Firestore delete() fails, deleteProfile should not attempt to
     * delete notification preferences.
     */
    @Test
    public void deleteProfile_FirestoreFailure_DoesNotDeleteNotificationPreference() {
        String email = "failed.delete@example.com";

        DocumentReference profileRef = mock(DocumentReference.class);
        when(mockProfilesCollection.document(email)).thenReturn(profileRef);

        Task<Void> mockDeleteProfileTask = mock(Task.class);
        when(profileRef.delete()).thenReturn(mockDeleteProfileTask);

        Exception firestoreException = new Exception("Delete failed");

        doAnswer((Answer<Task<Void>>) invocation -> mockDeleteProfileTask)
                .when(mockDeleteProfileTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnFailureListener failureListener =
                    invocation.getArgument(0);
            failureListener.onFailure(firestoreException);
            return mockDeleteProfileTask;
        }).when(mockDeleteProfileTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        loadUploadProfileModel.deleteProfile(email);

        // Assert - profile deletion attempted
        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(email);
        verify(profileRef, atLeastOnce()).delete();

        // Notification preferences are never deleted on failure
        verify(mockFirestore, never()).collection("NotificationPreferences");
        verify(mockNotificationPrefsCollection, never()).document(any(String.class));
    }
}



