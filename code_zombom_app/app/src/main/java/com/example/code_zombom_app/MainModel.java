package com.example.code_zombom_app;

import com.example.code_zombom_app.MVC.TModel;
import com.example.code_zombom_app.MVC.TView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * The idea for now is this is the MAIN model ("brain") of our project. Every other activity should
 * be a "view" or "controller"
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see com.example.code_zombom_app.MVC.TModel
 */
public class MainModel extends TModel<TView> {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                .addOnSuccessListener(aVoid -> notifyViews())
                .addOnFailureListener(Throwable::printStackTrace);
    }

    /**
     * Find and load a profile from the database
     *
     * @param email The email to find a Profile with
     * @return A profile that is loaded from the database. Null if the email address is not
     *         associated with any profile
     */
    public Profile loadProfile(String email) {
        db.collection("Profiles").document(email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Profile profile;
                        String type = snapshot.getString("Type");

                        class GeneralProfile {

                            /**
                             * Define a general nested method to load profile to avoid repetition.
                             * Use this after the Profile object's constructor is called
                             *
                             * @param profile The already initialized profile
                             */
                            void loadGeneral(Profile profile) {
                                profile.setName(snapshot.getString("Name"));
                                profile.setPhone(snapshot.getString("Phone"));
                                profile.setDeviceId(snapshot.getString("Device ID"));
                            }
                        }

                        if (type.equals("Entrant")) {
                            profile = new Entrant(snapshot.getString("Email"));
                            new GeneralProfile().loadGeneral(profile);
                            (Entrant) profile.se
                        }
                    }
                })
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
