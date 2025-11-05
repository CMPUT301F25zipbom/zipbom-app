package com.example.code_zombom_app.MVC;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A general model for this project
 *
 * @author Dang Nguyen
 * @version 1.0.0 11/5/2025
 * @see TModel
 */
public abstract class GModel extends TModel<TView> {
    protected final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public enum State {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        SIGNUP_SUCCESS,
        SIGNUP_FAILURE,

        NEUTRAL
        /* More to be implemented later */
    }
    protected State state;

    public GModel() {
        state = State.NEUTRAL;
    }

    /**
     * @return The current state of the model
     */
    public State getState() {
        return state;
    }
}
