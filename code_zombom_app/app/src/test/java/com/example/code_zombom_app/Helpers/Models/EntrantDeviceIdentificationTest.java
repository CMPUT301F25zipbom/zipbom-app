package com.example.code_zombom_app.Helpers.Models;

import android.content.Context;

import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Device identification login/registration tests (US 01.07.01).
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrantDeviceIdentificationTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockProfilesCollection;
    @Mock private Query mockDeviceIdQuery;
    @Mock private QuerySnapshot mockQuerySnapshot;
    @Mock private DocumentSnapshot mockDocumentSnapshot;

    private static final String DEVICE_ID = "device-123";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.whereArrayContains("deviceId", DEVICE_ID)).thenReturn(mockDeviceIdQuery);
    }

    @Test
    public void addDeviceId_NoExistingMapping_AddsDeviceAndSucceeds() {
        TestableLoadUploadProfileModel model = new TestableLoadUploadProfileModel(mockFirestore, DEVICE_ID);
        Profile profile = new Entrant("Name", "email@example.com", "111-1111");

        Task<QuerySnapshot> mockQueryTask = mockTaskWithCompletion(true);
        when(mockDeviceIdQuery.get()).thenReturn(mockQueryTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        model.addDeviceId(null, profile);

        assertTrue(profile.isDeviceIdLinked(DEVICE_ID));
        assertEquals(GModel.State.ADD_DEVICE_ID_SUCCESS, model.lastNotifiedState);
        assertEquals(DEVICE_ID, model.lastMessage);
    }

    @Test
    public void addDeviceId_DuplicateMapping_FailsGracefully() {
        TestableLoadUploadProfileModel model = new TestableLoadUploadProfileModel(mockFirestore, DEVICE_ID);
        Profile profile = new Entrant("Name", "email@example.com", "111-1111");

        Task<QuerySnapshot> mockQueryTask = mockTaskWithCompletion(true);
        when(mockDeviceIdQuery.get()).thenReturn(mockQueryTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);

        model.addDeviceId(null, profile);

        assertEquals(GModel.State.ADD_DEVICE_ID_FAILURE, model.lastNotifiedState);
        assertEquals("The device Id " + DEVICE_ID + " is already linked withanother profile!", model.getErrorMsg());
    }

    @Test
    public void loadProfileWithDeviceId_SuccessLoadsEntrant() {
        TestableLoadUploadProfileModel model = new TestableLoadUploadProfileModel(mockFirestore, DEVICE_ID);

        Task<QuerySnapshot> mockGetTask = mockTaskWithSuccess();
        when(mockDeviceIdQuery.get()).thenReturn(mockGetTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.singletonList(mockDocumentSnapshot));
        when(mockDocumentSnapshot.getString("type")).thenReturn("Entrant");
        Entrant expected = new Entrant("Loaded", "loaded@example.com", "999-9999");
        when(mockDocumentSnapshot.toObject(Entrant.class)).thenReturn(expected);

        model.loadProfileWithDeviceId((Context) null);

        assertEquals(GModel.State.LOGIN_SUCCESS, model.lastNotifiedState);
        assertSame(expected, model.lastProfile);
    }

    @Test
    public void loadProfileWithDeviceId_NoMatch_FailureState() {
        TestableLoadUploadProfileModel model = new TestableLoadUploadProfileModel(mockFirestore, DEVICE_ID);

        Task<QuerySnapshot> mockGetTask = mockTaskWithSuccess();
        when(mockDeviceIdQuery.get()).thenReturn(mockGetTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        model.loadProfileWithDeviceId((Context) null);

        assertEquals(GModel.State.LOGIN_FAILURE, model.lastNotifiedState);
    }

    private Task<QuerySnapshot> mockTaskWithCompletion(boolean successful) {
        Task<QuerySnapshot> mockTask = (Task<QuerySnapshot>) org.mockito.Mockito.mock(Task.class);
        when(mockTask.addOnCompleteListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
                    com.google.android.gms.tasks.OnCompleteListener<QuerySnapshot> listener = invocation.getArgument(0);
                    when(mockTask.isSuccessful()).thenReturn(successful);
                    when(mockTask.getResult()).thenReturn(mockQuerySnapshot);
                    listener.onComplete(mockTask);
                    return mockTask;
                });
        when(mockTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask);
        return mockTask;
    }

    private Task<QuerySnapshot> mockTaskWithSuccess() {
        Task<QuerySnapshot> mockTask = (Task<QuerySnapshot>) org.mockito.Mockito.mock(Task.class);
        when(mockTask.addOnSuccessListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> {
                    com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
                    listener.onSuccess(mockQuerySnapshot);
                    return mockTask;
                });
        when(mockTask.addOnFailureListener(any()))
                .thenAnswer((Answer<Task<QuerySnapshot>>) invocation -> mockTask);
        return mockTask;
    }

    private static class TestableLoadUploadProfileModel extends LoadUploadProfileModel {
        private final String fakeDeviceId;
        private GModel.State lastNotifiedState;
        private Object lastMessage;
        private Object lastProfile;

        TestableLoadUploadProfileModel(FirebaseFirestore db, String deviceId) {
            super(db);
            this.fakeDeviceId = deviceId;
        }

        @Override
        public String getDeviceId(Context context) {
            return fakeDeviceId;
        }

        @Override
        public void notifyViews() {
            lastNotifiedState = this.state;
            lastMessage = getInterMsg("Message");
            lastProfile = getInterMsg("Profile");
        }
    }
}


