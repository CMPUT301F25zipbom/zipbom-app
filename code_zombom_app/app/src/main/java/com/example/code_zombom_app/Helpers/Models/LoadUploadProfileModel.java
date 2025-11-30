package com.example.code_zombom_app.Helpers.Models;

import com.example.code_zombom_app.Helpers.Location.Location;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoadUploadProfileModel extends GModel {
    protected FirebaseFirestore db;
    protected static final String errorTag = "FireBaseFireStore Error"; // Tag to debug errors
    public LoadUploadProfileModel(FirebaseFirestore db) {
        super();
        this.db = db; // Force the database to be initialized within a context
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

                        Profile profile = null;
                        String type = snapshot.getString("type");
                        if ("Entrant".equals(type)) {
                            // Keep entrant-specific fields intact by deserializing into Entrant
                            try {
                                profile = snapshot.toObject(Entrant.class);
                            } catch (RuntimeException e) {
                                Log.e("Loading Profile Error", "Silently ignoring documents" +
                                        "that not convertible to type Entrant", e);
                            }
                        } else {
                            profile = snapshot.toObject(Profile.class);
                        }

                        if (profile != null) {
                            setInterMsg("Profile", profile);
                        }
                        notifyViews();
                    } else {
                        state = State.LOGIN_FAILURE;
                        errorMsg = "Cannot find profile!";
                        notifyViews();
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(errorTag, "Load Profile Failure", e);
                    state = State.LOGIN_FAILURE;
                    errorMsg = "Cannot query the database!";
                    notifyViews();
                });
    }

    /**
     * Create a new profile and upload it onto the database
     *
     * @param name     The name of the profile
     * @param email    The email address associate with this profile
     * @param phone    The phone number associate with this profile
     * @param location The location associate with this profile
     * @param type     The type of profile
     */
    public void uploadProfile(String name, String email, String phone, Location location, String type) {
        resetState();
        Profile profile = null;

        try {
            if (type.equals("Entrant"))
                profile = new Entrant(name, email, phone);
            if (location != null) {
                assert profile != null;
                profile.setLocation(location);
            }
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
                                    syncNotificationPreference(finalProfile);
                                    state = State.SIGNUP_SUCCESS;
                                    notifyViews();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(errorTag, "Upload error", e);
                                    state = State.SIGNUP_FAILURE;
                                    errorMsg = "Cannot add profile to the database!";
                                    notifyViews();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(errorTag, "Querying error", e);
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
                                        syncNotificationPreference(newProfile);
                                        state = State.EDIT_PROFILE_SUCCESS;
                                        setInterMsg("Profile", newProfile);
                                        notifyViews();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(errorTag, "Update Profile Error", e);
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
                        Log.e(errorTag, "Querying error", e);
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
                                        deleteNotificationPreference(oldProfile.getEmail());
                                        // Now upload the new profile
                                        db.collection("Profiles").document(newProfile.getEmail())
                                                .set(newProfile)
                                                .addOnSuccessListener(aVoid2 -> {
                                                    syncNotificationPreference(newProfile);
                                                    state = State.EDIT_PROFILE_SUCCESS;
                                                    setInterMsg("Profile", newProfile);
                                                    notifyViews();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(errorTag, "Upload Edited Profile Error", e);
                                                    state = State.EDIT_PROFILE_FAILURE;
                                                    errorMsg = "Cannot upload the edited profile to the database!";
                                                    notifyViews();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(errorTag, "Delete Old Profile Error", e);
                                        state = State.EDIT_PROFILE_FAILURE;
                                        errorMsg = "Cannot delete the old profile from the database!";
                                        notifyViews();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(errorTag, "Querying Error", e);
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
                    deleteNotificationPreference(email);
                    state = State.DELETE_PROFILE_SUCCESS;
                    setInterMsg("Message", email);
                    notifyViews();
                })
                .addOnFailureListener(e -> {
                    Log.e(errorTag, "Deleting Old Profile Error", e);
                    state = State.DELETE_PROFILE_FAILURE;
                    errorMsg = "Cannot delete this profile from the database!";
                });
    }

    /**
     * @param context The context to get the device id
     * @return The device Id that this profile is using
     */
    public String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    protected void syncNotificationPreference(@Nullable Profile profile) {
        if (!(profile instanceof Entrant)) {
            return;
        }
        if (profile == null || profile.getEmail() == null) {
            return;
        }
        String normalized = normalizeNotificationKey(profile.getEmail());
        if (normalized.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", profile.getEmail().trim());
        payload.put("notificationEnabled", ((Entrant) profile).isNotificationEnabled());
        db.collection("NotificationPreferences")
                .document(normalized)
                .set(payload);
    }

    protected void deleteNotificationPreference(@Nullable String email) {
        String normalized = normalizeNotificationKey(email);
        if (normalized.isEmpty()) {
            return;
        }
        db.collection("NotificationPreferences")
                .document(normalized)
                .delete();
    }

    protected String normalizeNotificationKey(@Nullable String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Get the unique device Id of an android device and put it in a profile if the deviceId
     * is not associated with other account. Set the state to ADD_DEVICE_ID_SUCCESS upon success,
     * ADD_DEVICE_ID_FAILURE otherwise
     *
     * @param context The activity context which to get the device Id from
     * @param profile The profile to put the device Id in

     */
    public void addDeviceId(Context context, Profile profile) {
        resetState();
        String id = getDeviceId(context);

        /* Checked if the device id is already linked with another profile */
        db.collection("Profiles")
                .whereArrayContains("deviceId", id)
                        .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() &&
                                    task.getResult() != null &&
                                    !task.getResult().isEmpty()) {
                                        state = State.ADD_DEVICE_ID_FAILURE;
                                        errorMsg = "The device Id " + id + " is already linked with" +
                                                "another profile!";
                                        notifyViews();
                                    }
                                    else {
                                        profile.addDeviceId(id);
                                        state = State.ADD_DEVICE_ID_SUCCESS;
                                        setInterMsg("Message", id);
                                        notifyViews();
                                    }
                                })
                .addOnFailureListener(e -> {
                    Log.e(errorTag, "Querying Error", e);
                    state = State.ADD_DEVICE_ID_FAILURE;
                    errorMsg = "Cannot query the database for the device id " + id + "!";
                    notifyViews();
                });
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

    /**
     * Provide an additional method of logging in with a device Id. Set the state to LOGIN_SUCCESS or
     * LOGIN_FAILURE
     *
     * @param context The context to get the device Id
     */
    public void loadProfileWithDeviceId(Context context) {
        String deviceId = getDeviceId(context);

        db.collection("Profiles")
                .whereArrayContains("deviceId", deviceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String type = doc.getString("type");

                        Profile profile = null;
                        if ("Entrant".equals(type))
                            profile = doc.toObject(Entrant.class);

                        if (profile != null) {
                            state = State.LOGIN_SUCCESS;
                            setInterMsg("Profile", profile);
                            notifyViews();
                        }
                        else {
                            state = State.LOGIN_FAILURE;
                            errorMsg = "Cannot find any non-empty profile!";
                            notifyViews();
                        }
                    }
                    else {
                        state = State.LOGIN_FAILURE;
                        errorMsg = "Cannot find any profile associated with this device!";
                        notifyViews();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(errorTag, "Querying Error", e);
                    state = State.LOGIN_FAILURE;
                    errorMsg = "Error in querying the database";
                    notifyViews();
                });
    }

    /**
     * Send a request to login with the device Id
     */
    public void requestLoginWithDeviceId() {
        state = State.REQUEST_LOGIN_WITH_DEVICE_ID;
        notifyViews();
    }
}
