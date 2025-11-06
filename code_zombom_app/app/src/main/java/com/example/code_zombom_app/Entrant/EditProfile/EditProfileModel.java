package com.example.code_zombom_app.Entrant.EditProfile;

import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.MockUpEntrant;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfileModel extends LoadUploadProfileModel {
    private EditProfileRequest editState;
    private final Entrant currentEntrant;
    private final String email;
    private MockUpEntrant newEntrant;

    public enum EditProfileRequest {
        POPUP_EDIT,
        NEUTRAL
    }

    /**
     * Use this class to tell the UI how to create a popup window to edit the name, email, and phone.
     */
    public class PopUpEdit {
        private final String title;
        private final String request;
        private final String type;

        public PopUpEdit(String title, String request, String type) {
            this.title = title;
            this.request = request;
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public String getRequest() {
            return request;
        }

        public String getType() {
            return type;
        }
    }

    public EditProfileModel(FirebaseFirestore db, String email) {
        super(db);
        this.email = email;

        /* Load the current profile from the database */
        loadProfile(email);
        currentEntrant = (Entrant) getInterMsg("Profile");
        newEntrant = new MockUpEntrant(currentEntrant);
    }

    /**
     * Request to create a popup window. Send the request through a PopUpEdit object, which is sent
     * through the Request channel of interMsg
     *
     * @param title   The title of the popup window
     * @param message The message to ask the users
     * @param editType What to edit: The name, email address, or the phone number
     */
    public void requestPopUp(String title, String message, String editType) {
        resetState();
        state = State.REQUEST;
        editState = EditProfileRequest.POPUP_EDIT;
        setInterMsg("Request", new PopUpEdit(title, message, editType));
        notifyViews();
    }

    /**
     * @return The request state
     */
    public EditProfileRequest getEditState() {
        return editState;
    }

    @Override
    public void resetState() {
        super.resetState();
        editState = EditProfileRequest.NEUTRAL;
    }

    /**
     * Edit the name of the profile
     *
     * @param name New name of the new profile
     */
    public void editName(String name) {
        if (!name.equals(newEntrant.getName())) {
            newEntrant.setName(name);
        }
    }

    /**
     * Edit the email of the profile
     *
     * @param email New email for the new profile
     */
    public void editEmail(String email) {
        if (!email.equals(newEntrant.getEmail())) {
            newEntrant.setEmail(email);
        }
    }

    /**
     * Edit the phone number of the profile
     *
     * @param phone New phone number for the new profile
     */
    public void editPhone(String phone) {
        if (!phone.equals(newEntrant.getPhone())) {
            newEntrant.setPhone(phone);
        }
    }

    /**
     * Edit the entrant's profile
     */
    public void editEntrant() {
        editProfile(currentEntrant, newEntrant);
    }

    /**
     * Delete the entrant from the database
     */
    public void deleteEntrant() {
        deleteProfile(email);
    }

    /**
     * @return The entrant's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the notification flag in the Entrant profile
     *
     * @param notification The notification flag to set
     */
    public void setNotification(Boolean notification) {
        newEntrant.setNotificationsEnabled(notification);
        state = State.NOTIFICATION_TOGGLE;
        setInterMsg("Message", notification);
        notifyViews();
    }
}
