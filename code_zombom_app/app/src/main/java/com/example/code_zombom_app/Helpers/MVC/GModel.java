package com.example.code_zombom_app.Helpers.MVC;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

/**
 * A general model for this project
 *
 * @author Dang Nguyen
 * @version 1.0.0 11/5/2025
 * @see TModel
 */
public abstract class GModel extends TModel<TView> {
    protected String errorMsg; // Used to display the error message

    /**
     * @deprecated For simplicity, in the future just create a getters to retrieve an elements from
     *             the model. This sh*t has become stupid and redundant
     */
    private final HashMap<String, Object> interMsg; // Used to store the intermediate message (information)

    private final String[] allowedKeys = {
            "Profile", "Request", "Message", "Extra"
    };

    private static GModel currentModel; // The model to pass around

    public enum State {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOG_OUT,
        SIGNUP_SUCCESS,
        SIGNUP_FAILURE,
        SIGNUP_ADDRESS_SUCCESS,
        SIGNUP_ADDRESS_FAILURE,
        OPEN, // Use this state when you want to open an activity
        CLOSE, // Use this state when you want to go back from an activity
        REQUEST,
        REQUEST_TOGGLE_LINK_DEVICE_ID,
        REQUEST_DELETE_PROFILE,
        REQUEST_LOGIN_WITH_DEVICE_ID,
        EDIT_PROFILE_SUCCESS,
        EDIT_PROFILE_FAILURE,
        DELETE_PROFILE_SUCCESS,
        DELETE_PROFILE_FAILURE,
        NOTIFICATION_TOGGLE,
        ADD_DEVICE_ID_SUCCESS,
        ADD_DEVICE_ID_FAILURE,
        REMOVE_DEVICE_ID,


        INTERNAL_ERROR,
        NEUTRAL
        /* More to be implemented later when needed */
    }
    protected State state;

    public GModel() {
        resetState();
        interMsg = new HashMap<>();
        for (String key : allowedKeys)
            interMsg.put(key, null);
    }

    /**
     * Set the current using model.
     *
     * @param model The current model in used
     * @throws IllegalArgumentException If the {@code model} is null
     * @throws RuntimeException If there is already another model in use
     */
    public static void setCurrentModel(GModel model) {
        if (model == null)
            throw new RuntimeException("Current Model in used cannot be null!");

        if (currentModel == null)
            currentModel = model;
        else
            throw new RuntimeException("Another model is already in used!");
    }

    /**
     * @return The current model in used, null if there is no model currently in use
     */
    public static GModel getCurrentModel() {
        return currentModel;
    }

    /**
     * Manually reset the current model
     */
    public static void resetCurrentModel() {
        currentModel = null;
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
     * Put a message to the intermediate message map
     *
     * @param msg The message object to put into the map
     * @throws IllegalArgumentException if the key is not one of the allowed entry type or the message
     *                                  is null
     */
    protected void setInterMsg(String key, Object msg) {
        boolean allowed = false;

        for (String s : allowedKeys)
            if (s.equals(key)) {
                allowed = true;
                break;
            }

        if (!allowed) {
            state = State.INTERNAL_ERROR;
            errorMsg = "Key Type " + "Email" + " is not allowed!";
            throw new IllegalArgumentException(errorMsg);
        }
        if (msg == null) {
            state = State.INTERNAL_ERROR;
            errorMsg = "Message object cannot be null!";
            throw new IllegalArgumentException(errorMsg);
        }

        interMsg.put(key, msg);
    }

    /**
     * @return An intermediate message's object
     */
    public Object getInterMsg(String key) {
        return interMsg.get(key);
    }

    /**
     * Reset the state back to NEUTRAL and the error message to null
     */
    protected void resetState() {
        state = State.NEUTRAL;
        errorMsg = null;
        if (interMsg != null)
            interMsg.replaceAll((k, v) -> null);
    }

    /**
     * Set the state to a new state
     *
     * @param state The state to set
     */
    protected void setState(State state) {
        resetState();
        this.state = state;
    }

    /**
     * Declare that you want to open a new activity
     */
    public void open() {
        state = State.OPEN;
        notifyViews();
    }

    /**
     * Declare that you want to close (go back from) an activity
     */
    public void close() {
        state = State.CLOSE;
        notifyViews();
    }

    /**
     * Additional opening method that help you send additional intermediate message. The message
     * always uses key "Extra" to avoid overwriting previously written data used by other process
     *
     * @param msg The extra message object to send
     */
    public void open(Object msg) {
        setInterMsg("Extra", msg);
        open();
    }

    /**
     * Declare that you want to log out
     */
    public void logout() {
        state = State.LOG_OUT;
        notifyViews();
    }
}
