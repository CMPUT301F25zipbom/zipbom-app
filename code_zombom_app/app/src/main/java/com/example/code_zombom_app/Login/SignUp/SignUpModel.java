package com.example.code_zombom_app.Login.SignUp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.code_zombom_app.MVC.GModel;
import com.example.code_zombom_app.Objects.Entrant;
import com.example.code_zombom_app.Objects.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpModel extends GModel {

    public SignUpModel(FirebaseFirestore db) {
        super(db);
    }

    /**
     * Create a new profile and upload it onto the database
     *
     * @param name  The name of the profile
     * @param email The email address associate with this profile
     * @param phone The phone number associate with this profile
     * @param type  The type of profile
     */
    public void setProfile(String name, String email, String phone, String type) {
        resetState();
        Profile profile = null;

        try {
            if (type.equals("Entrant"))
                profile = new Entrant(name, email, phone);
        }
        catch (IllegalArgumentException e) {
            state = State.SIGNUP_FAILURE;
            errorMsg = "The email address cannot be null, empty or blank!";
            notifyViews();
            return;
        }

        /* Check to see if this email address is already in the database. If not then add a new
        * profile to the database
        */
        Profile finalProfile = profile;
        db.collection("Profiles").document(email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        state = State.SIGNUP_FAILURE;
                        errorMsg = "This email address has already been associated with another account";
                        notifyViews();
                    }
                    else {
                        db.collection("Profiles").document(finalProfile.getEmail())
                                .set(finalProfile)
                                .addOnSuccessListener(aVoid -> {
                                    state = State.SIGNUP_SUCCESS;
                                    notifyViews();
                                })
                                .addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    state = State.SIGNUP_FAILURE;
                                    errorMsg = "Cannot add profile to the database!";
                                    notifyViews();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    state = State.LOGIN_FAILURE;
                    errorMsg = "Error in querying the database!";
                    notifyViews();
                });
    }
}
