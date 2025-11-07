package com.example.code_zombom_app.Helpers.Models;

import android.content.Context;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoadUploadProfileModel extends GModel {
    public LoadUploadProfileModel(FirebaseFirestore db) {
        super(db);
    }

    /**
     * Check if an email address is in the database. If the input email address is in the database
     * then return the associated profile through intermsg
     *
     * @param email The email of the profile to load; used as the document ID in Firestore
     * @throws IllegalArgumentException If the method setInterMsg failed
     * @see Profile
     * @see Entrant
     * @see FirebaseFirestore
     * @see GModel
     */
    public void loadProfile(String email) throws IllegalArgumentException {
        resetState();

        if (email == null || email.trim().isEmpty()) {
            state = State.LOGIN_FAILURE;
            errorMsg = "Cannot find profile!";
            notifyViews();
            return;
        }

        db.collection("Profiles").document(email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        state = State.LOGIN_SUCCESS;
//                        Profile profile = (Profile) snapshot.toObject(Profile.class);
                        //assert profile != null;
                        if (snapshot.getString("type").equals("Entrant")) {
                            Entrant entrant = snapshot.toObject(Entrant.class);
                            setInterMsg("Profile", entrant);
                        }
                        setInterMsg("Profile", (Profile) snapshot.toObject(Profile.class));
                        notifyViews();
                    } else {
                        state = State.LOGIN_FAILURE;
                        errorMsg = "Cannot find profile!";
                        notifyViews();
                    }

                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    state = State.LOGIN_FAILURE;
                    errorMsg = "Cannot query the database!";
                    notifyViews();
                });
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

    /**
     * Edit a Profile in the database.
     * This method sets the state to EDIT_PROFILE_SUCCESS or EDIT_PROFILE_FAILURE.
     * Right now this method will upload a new Profile onto the database regardless of if there is
     * any change to the Profile.
     *
     * @param oldProfile The old profile
     * @param newProfile The new profile to replace the old one
     * @see Profile
     */
    public void editProfile(Profile oldProfile, Profile newProfile) {
        resetState();

        assert oldProfile != null && newProfile != null;

        // If email didn't change → just update the profile document
        if (oldProfile.getEmail().equals(newProfile.getEmail())) {
            db.collection("Profiles").document(oldProfile.getEmail())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            db.collection("Profiles").document(oldProfile.getEmail())
                                    .set(newProfile)
                                    .addOnSuccessListener(aVoid -> {
                                        state = State.EDIT_PROFILE_SUCCESS;
                                        setInterMsg("Profile", newProfile);
                                        notifyViews();
                                    })
                                    .addOnFailureListener(e -> {
                                        e.printStackTrace();
                                        state = State.EDIT_PROFILE_FAILURE;
                                        errorMsg = "Cannot update profile in the database!";
                                        notifyViews();
                                    });
                        } else {
                            state = State.EDIT_PROFILE_FAILURE;
                            errorMsg = "Profile not found in the database!";
                            notifyViews();
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        state = State.EDIT_PROFILE_FAILURE;
                        errorMsg = "Error querying the database!";
                        notifyViews();
                    });
        }
        else {

            // If email changed → check new email first, then delete old and add new
            db.collection("Profiles").document(newProfile.getEmail())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            state = State.EDIT_PROFILE_FAILURE;
                            errorMsg = "This email address is already associated with another profile!";
                            notifyViews();
                        } else {
                            // Delete old document
                            db.collection("Profiles").document(oldProfile.getEmail())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Now upload the new profile
                                        db.collection("Profiles").document(newProfile.getEmail())
                                                .set(newProfile)
                                                .addOnSuccessListener(aVoid2 -> {
                                                    state = State.EDIT_PROFILE_SUCCESS;
                                                    setInterMsg("Profile", newProfile);
                                                    notifyViews();
                                                })
                                                .addOnFailureListener(e -> {
                                                    e.printStackTrace();
                                                    state = State.EDIT_PROFILE_FAILURE;
                                                    errorMsg = "Cannot upload the edited profile to the database!";
                                                    notifyViews();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        e.printStackTrace();
                                        state = State.EDIT_PROFILE_FAILURE;
                                        errorMsg = "Cannot delete the old profile from the database!";
                                        notifyViews();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        state = State.EDIT_PROFILE_FAILURE;
                        errorMsg = "Failure querying the database for the new email address!";
                        notifyViews();
                    });
        }
    }

    /**
     * Delete a profile from the database. Set the state to DELETE_PROFILE_SUCCESS or
     * DELETE_PROFILE_FAILURE
     *
     * @param email The email address that associate with the profile to delete
     */
    public void deleteProfile(String email) {
        resetState();

        assert email != null;

        db.collection("Profiles").document(email)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    state = State.DELETE_PROFILE_SUCCESS;
                    setInterMsg("Message", email);
                    notifyViews();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    state = State.DELETE_PROFILE_FAILURE;
                    errorMsg = "Cannot delete this profile from the database!";
                });
    }

    /**
     * Get the unique device Id of an android device and put it in a profile. Set the state to
     * ADD_DEVICE_ID upon success
     *
     * @param context The activity context which to get the device Id from
     * @param profile The profile to put the device Id in

     */
    public void addDeviceId(Context context, Profile profile) {
        resetState();
        String id = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        profile.addDeviceId(id);
        state = State.ADD_DEVICE_ID;
        setInterMsg("Message", id);
        notifyViews();
    }

    /**
     * Remove the device Id from a profile. Set the state to REMOVE_DEVICE_ID upon success
     *
     * @param id      The device id to be removed
     * @param profile The profile to remove the device id from
     */
    public void removeDeviceId(String id, Profile profile) {
        resetState();
        profile.removeDeviceId(id);
        state = State.REMOVE_DEVICE_ID;
        notifyViews();
    }
}
