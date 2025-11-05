package com.example.code_zombom_app;

import com.example.code_zombom_app.Objects.Profile;

/**
 * A callback interface to handle asynchronous loading of an object the database.
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see Profile
 * @see com.google.firebase.firestore.FirebaseFirestore
 */
public interface FireStoreCallBack {
    void onProfileLoaded(Profile profile);
    void onProfileExist(boolean exist);
}
