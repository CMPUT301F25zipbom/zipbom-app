package com.example.code_zombom_app.MVC;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A general model for this project
 *
 * @author Dang Nguyen
 * @version 1.0.0 11/5/2025
 * @see TModel
 */
public abstract class GModel extends TModel<TView> {
    protected FirebaseFirestore db;
    protected String errorMsg;
    public enum State {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        SIGNUP_SUCCESS,
        SIGNUP_FAILURE,

        NEUTRAL
        /* More to be implemented later */
    }
    protected State state;

    public GModel(FirebaseFirestore db) {
        resetState();
        this.db = db;
    }

    /**
     * @return The current state of the model
     */
    public State getState() {
        return state;
    }

    /**
     * @return The error message
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Reset the state back to NEUTRAL and the error message to null
     */
    public void resetState() {
        state = State.NEUTRAL;
        errorMsg = null;
    }
}
