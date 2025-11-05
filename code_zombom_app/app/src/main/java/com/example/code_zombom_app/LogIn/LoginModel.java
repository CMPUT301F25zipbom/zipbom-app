package com.example.code_zombom_app.LogIn;

import com.example.code_zombom_app.Entrant;
import com.example.code_zombom_app.FireStoreCallBack;
import com.example.code_zombom_app.MVC.TModel;
import com.example.code_zombom_app.MVC.TView;
import com.example.code_zombom_app.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * The idea for now is this is the MAIN model ("brain") of our project. Every other activity should
 * be a "view" or "controller"
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see TModel
 */
public class LoginModel extends TModel<TView> {
    public enum State {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        SIGNUP_SUCCESS,
        SIGNUP_FAILURE,

        NEUTRAL
        /* More to be implemented later */
    }

    private State state;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LoginModel() {
        state = State.NEUTRAL;
    }

    /**
     * @return The current state of the model
     */
    public State getState() {
        return state;
    }

    /**
     * Create a new profile
     *
     * @param profile The new profile to create
     * @see Profile
     */
    public void setProfile(Profile profile) {
        Map<String, Object> data = new HashMap<>();
        data.put("Name", profile.getName());
        data.put("Email", profile.getEmail());
        data.put("Phone", profile.getPhone());
        data.put("Type", profile.getType());
        data.put("Device ID", profile.getDeviceId());

        if (profile.getType().equals("Entrant"))
            setEntrant(data, (Entrant) profile);

        db.collection("Profiles").document(profile.getEmail())
                .set(data)
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

//    /**
//     * Loads a {@link Profile} from the Firestore database asynchronously.
//     * <p>
//     * This method fetches the profile document from the "Profiles" collection using the provided
//     * email as the document ID. If a matching document exists, it constructs a {@link Profile} object
//     * (or an {@link Entrant} object if the profile type is "Entrant") and notifies the provided
//     * {@link ProfileCallBack}. If no profile exists or an error occurs during the fetch, the callback
//     * is invoked with {@code null}.
//     * </p>
//     *
//     * <p><b>Note:</b> Firestore operations are asynchronous, so the {@code callback} is necessary
//     * to handle the result after the fetch completes.</p>
//     *
//     * @param email the email of the profile to load; used as the document ID in Firestore
//     * @param callback a {@link ProfileCallBack} to receive the loaded profile or {@code null} if
//     *                 the profile does not exist or if an error occurs
//     * @see Profile
//     * @see Entrant
//     * @see ProfileCallBack
//     */
//    public void loadProfile(String email, ProfileCallBack callback) {
//        db.collection("Profiles").document(email)
//                .get()
//                .addOnSuccessListener(snapshot -> {
//                    if (snapshot.exists()) {
//                        Profile profile = null;
//
//                        String name = snapshot.getString("Name");
//                        String Email = snapshot.getString("Email");
//                        String phone = snapshot.getString("Phone");
//                        String type = snapshot.getString("Type");
//                        String deviceID = snapshot.getString("Device ID");
//
//                        assert type != null;
//                        if (type.equals("Entrant")) {
//                            ArrayList<String> eventHistory = (ArrayList<String>) snapshot.get("Event History");
//                            ArrayList<String> waitingEvents = (ArrayList<String>) snapshot.get("Waiting Events");
//                            assert eventHistory != null;
//                            assert waitingEvents != null;
//                            profile = new Entrant(name, Email, phone, eventHistory, waitingEvents);
//                        }
//                        notifyViews();
//                        callback.onProfileLoaded(profile); // Return the profile this way since Firestore is async
//                    }
//                    else {
//                        callback.onProfileLoaded(null);
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    e.printStackTrace();
//                    callback.onProfileLoaded(null);
//                });
//    }

    /**
     * Check if an email address is in the database.
     *
     * @param email the email of the profile to load; used as the document ID in Firestore
     *
     * @see Profile
     * @see Entrant
     * @see FirebaseFirestore
     */
    public void loadProfile(String email) {
        db.collection("Profiles").document(email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        state = State.LOGIN_SUCCESS;
                        notifyViews();
                    }
                    else {
                        state = State.LOGIN_FAILURE;
                        notifyViews();
                    }

                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    state = State.LOGIN_FAILURE;
                    notifyViews();
                });
    }

    /**
     * Handle putting the entrant in a data Hashmap
     *
     * @param data    The Hashmap to put the data in
     * @param entrant The entrant to be put in the database
     * @see Map
     * @see HashMap
     * @see Entrant
     */
    private void setEntrant(Map data, Entrant entrant) {
        data.put("Event History", entrant.getEventHistory());
        data.put("Waiting Events", entrant.getWaitingEvents());
    }
}
