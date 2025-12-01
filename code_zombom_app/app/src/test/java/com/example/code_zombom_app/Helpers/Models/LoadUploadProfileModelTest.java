package com.example.code_zombom_app.Helpers.Models;

import com.example.code_zombom_app.Helpers.Location.Location;
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
 * Unit tests for LoadUploadProfileModel.uploadProfile() method.
 * Tests the feature for entrants to provide their personal information (US 01.02.01).
 *
 * @author Test Suite
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadUploadProfileModelTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockProfilesCollection;

    @Mock
    private DocumentReference mockProfileDocumentRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    private LoadUploadProfileModel loadUploadProfileModel;
    private static final String TEST_NAME = "John Doe";
    private static final String TEST_EMAIL = "john.doe@example.com";
    private static final String TEST_PHONE = "123-456-7890";
    private static final String PROFILE_TYPE_ENTRANT = "Entrant";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        loadUploadProfileModel = new LoadUploadProfileModel(mockFirestore);

        // Setup Firestore collection and document reference chain
        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(any(String.class))).thenReturn(mockProfileDocumentRef);
    }

    @Test
    public void uploadProfile_SuccessfulProfileCreation_WithAllFields() {
        when(mockDocumentSnapshot.exists()).thenReturn(false); // Email doesn't exist yet

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        // Mock the get() success callback
        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Mock the set() success callback
        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act
        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        // Assert - verify Firestore interactions
        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(TEST_EMAIL);
        verify(mockProfileDocumentRef, atLeastOnce()).get();

        // Verify set() was called with the profile
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(mockProfileDocumentRef, atLeastOnce()).set(profileCaptor.capture());

        Profile savedProfile = profileCaptor.getValue();
        assertNotNull("Saved profile should not be null", savedProfile);
        assertEquals("Profile name should match", TEST_NAME, savedProfile.getName());
        assertEquals("Profile email should match", TEST_EMAIL, savedProfile.getEmail());
        assertEquals("Profile phone should match", TEST_PHONE, savedProfile.getPhone());
    }

    @Test
    public void uploadProfile_OptionalPhoneNumber_NoPhoneProvided_Succeeds() {
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act - upload profile without phone number
        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, null, null, PROFILE_TYPE_ENTRANT);

        // Assert
        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(TEST_EMAIL);
        verify(mockProfileDocumentRef, atLeastOnce()).get();
        verify(mockProfileDocumentRef, atLeastOnce()).set(any(Profile.class));

        // Verify the profile was saved with null phone
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(mockProfileDocumentRef, atLeastOnce()).set(profileCaptor.capture());

        Profile savedProfile = profileCaptor.getValue();
        assertNotNull("Saved profile should not be null", savedProfile);
        assertEquals("Profile name should match", TEST_NAME, savedProfile.getName());
        assertEquals("Profile email should match", TEST_EMAIL, savedProfile.getEmail());
        assertEquals("Profile phone should be null", null, savedProfile.getPhone());
    }

    @Test
    public void uploadProfile_OptionalPhoneNumber_EmptyPhoneProvided_Succeeds() {
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Act - upload profile with empty phone number
        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, "", null, PROFILE_TYPE_ENTRANT);

        // Assert
        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfileDocumentRef, atLeastOnce()).set(any(Profile.class));
    }

    @Test
    public void uploadProfile_EmptyEmail_DoesNotWriteToFirestore() {
        String emptyEmail = "";

        loadUploadProfileModel.uploadProfile(TEST_NAME, emptyEmail, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, never()).collection("Profiles");
        verify(mockProfileDocumentRef, never()).get();
        verify(mockProfileDocumentRef, never()).set(any(Profile.class));
    }

    @Test
    public void uploadProfile_NullEmail_DoesNotWriteToFirestore() {
        String nullEmail = null;

        loadUploadProfileModel.uploadProfile(TEST_NAME, nullEmail, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, never()).collection("Profiles");
        verify(mockProfileDocumentRef, never()).get();
        verify(mockProfileDocumentRef, never()).set(any(Profile.class));
    }

    @Test
    public void uploadProfile_BlankEmail_DoesNotWriteToFirestore() {
        String blankEmail = "   ";

        loadUploadProfileModel.uploadProfile(TEST_NAME, blankEmail, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, never()).collection("Profiles");
        verify(mockProfileDocumentRef, never()).get();
        verify(mockProfileDocumentRef, never()).set(any(Profile.class));
    }

    @Test
    public void uploadProfile_EmailAlreadyExists_DoesNotCreateProfile() {
        when(mockDocumentSnapshot.exists()).thenReturn(true); // Email already exists

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(TEST_EMAIL);
        verify(mockProfileDocumentRef, atLeastOnce()).get();

        // Verify set() was NOT called (profile should not be created)
        verify(mockProfileDocumentRef, never()).set(any(Profile.class));
    }

    @Test
    public void uploadProfile_FirestoreWriteFailure_PropagatesError() {
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        Exception firestoreException = new Exception("Firestore write failed");

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        // Mock set() failure
        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnFailureListener failureListener =
                    invocation.getArgument(0);
            failureListener.onFailure(firestoreException);
            return mockSetTask;
        }).when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfileDocumentRef, atLeastOnce()).get();
        verify(mockProfileDocumentRef, atLeastOnce()).set(any(Profile.class));
        verify(mockSetTask, atLeastOnce()).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));
    }

    @Test
    public void uploadProfile_FirestoreQueryFailure_PropagatesError() {
        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Exception firestoreException = new Exception("Firestore query failed");

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnFailureListener failureListener =
                    invocation.getArgument(0);
            failureListener.onFailure(firestoreException);
            return mockGetTask;
        }).when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfileDocumentRef, atLeastOnce()).get();

        // Verify set() was NOT called (query failed before we could check)
        verify(mockProfileDocumentRef, never()).set(any(Profile.class));
        verify(mockGetTask, atLeastOnce()).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));
    }

    @Test
    public void uploadProfile_UsesCorrectFirestoreDocumentPath() {
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).document(TEST_EMAIL);
        verify(mockProfileDocumentRef, atLeastOnce()).get();
        verify(mockProfileDocumentRef, atLeastOnce()).set(any(Profile.class));
    }

    @Test
    public void uploadProfile_ProfileDataMapping_AllFieldsCorrect() {
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, null, PROFILE_TYPE_ENTRANT);

        // Assert - verify the profile object contains correct data
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(mockProfileDocumentRef, atLeastOnce()).set(profileCaptor.capture());

        Profile savedProfile = profileCaptor.getValue();
        assertNotNull("Saved profile should not be null", savedProfile);
        assertEquals("Profile name should match input", TEST_NAME, savedProfile.getName());
        assertEquals("Profile email should match input", TEST_EMAIL, savedProfile.getEmail());
        assertEquals("Profile phone should match input", TEST_PHONE, savedProfile.getPhone());

        if (savedProfile instanceof Entrant) {
            assertEquals("Profile type should be Entrant", "Entrant", savedProfile.getType());
        }
    }

    @Test
    public void uploadProfile_WithLocation_SetsLocationOnProfile() {
        Location testLocation = new Location();
        testLocation.setName("Test Location");
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> mockGetTask = mock(Task.class);
        when(mockProfileDocumentRef.get()).thenReturn(mockGetTask);

        Task<Void> mockSetTask = mock(Task.class);
        when(mockProfileDocumentRef.set(any(Profile.class))).thenReturn(mockSetTask);

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(mockDocumentSnapshot);
            return mockGetTask;
        }).when(mockGetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<DocumentSnapshot>>) invocation -> mockGetTask)
                .when(mockGetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> successListener =
                    invocation.getArgument(0);
            successListener.onSuccess(null);
            return mockSetTask;
        }).when(mockSetTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));

        doAnswer((Answer<Task<Void>>) invocation -> mockSetTask)
                .when(mockSetTask).addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class));

        loadUploadProfileModel.uploadProfile(TEST_NAME, TEST_EMAIL, TEST_PHONE, testLocation, PROFILE_TYPE_ENTRANT);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(mockProfileDocumentRef, atLeastOnce()).set(profileCaptor.capture());

        Profile savedProfile = profileCaptor.getValue();
        assertNotNull("Saved profile should not be null", savedProfile);
        assertNotNull("Profile location should be set", savedProfile.getLocation());
        assertEquals("Profile location should match input", testLocation, savedProfile.getLocation());
    }
}
