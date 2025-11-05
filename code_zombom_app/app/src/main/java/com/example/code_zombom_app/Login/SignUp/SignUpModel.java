package com.example.code_zombom_app.Login.SignUp;

import com.example.code_zombom_app.MVC.GModel;
import com.example.code_zombom_app.MVC.TModel;
import com.example.code_zombom_app.MVC.TView;
import com.example.code_zombom_app.Profile;

public class SignUpModel extends GModel {

    public SignUpModel() {
        super();
    }

    /**
     * Create a new profile
     *
     * @param profile The new profile to create
     * @see Profile
     */
    public void setProfile(Profile profile) {
        db.collection("Profiles").document(profile.getEmail())
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    state = State.SIGNUP_SUCCESS;
                    notifyViews();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    state = State.SIGNUP_FAILURE;
                    notifyViews();
                });
    }
}
