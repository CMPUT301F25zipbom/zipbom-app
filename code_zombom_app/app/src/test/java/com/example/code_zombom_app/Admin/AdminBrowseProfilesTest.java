package com.example.code_zombom_app.Admin;

import com.example.code_zombom_app.ui.admin.ProfileAdminFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminBrowseProfilesTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockProfilesCollection;

    private ProfileAdminFragment fragment;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fragment = new ProfileAdminFragment();

        Field dbField = ProfileAdminFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, mockFirestore);

        Field profilesField = ProfileAdminFragment.class.getDeclaredField("profilesDb");
        profilesField.setAccessible(true);
        profilesField.set(fragment, mockProfilesCollection);

        when(mockFirestore.collection("Profiles")).thenReturn(mockProfilesCollection);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            EventListener<QuerySnapshot> listener =
                    (EventListener<QuerySnapshot>) invocation.getArgument(0);
            // Do not invoke the listener to avoid UI dependencies.
            return null;
        }).when(mockProfilesCollection).addSnapshotListener(any(EventListener.class));
    }

    @Test
    public void loadProfilesFromDatabase_AdminBrowsesProfiles_RegistersSnapshotListener() throws Exception {
        java.lang.reflect.Method method = ProfileAdminFragment.class
                .getDeclaredMethod("loadProfilesFromDatabase");
        method.setAccessible(true);
        method.invoke(fragment);

        verify(mockFirestore, atLeastOnce()).collection("Profiles");
        verify(mockProfilesCollection, atLeastOnce()).addSnapshotListener(any(EventListener.class));
    }
}
