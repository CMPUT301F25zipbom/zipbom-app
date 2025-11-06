package com.example.code_zombom_app.Helpers.Users;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Represent a profile. Store the personal information such as email addresses, name, phone number,
 * personal identifier and device ID.
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 */
public class Profile {
    protected String name;
    protected String email;
    protected String phone;
    protected ArrayList<String> deviceId = new ArrayList<>();
    protected String type; // Admin, organizer or entrant

    /**
     * This constructor is required for Firestore deserialization
     */
    public Profile() {}

    /**
     * ALWAYS call this constructor. A profile MUST always be associated with an email address
     *
     * @param email The email address associated with this profile
     * @throws IllegalArgumentException If the email is null, blank or empty
     */
    public Profile(String email) {
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("Email cannot be null, blank or empty");
        this.email = email;
    }

    /**
     * Convenient constructor for quickly instantiating an entrant profile.
     *
     * @param name  entrant display name
     * @param email contact email
     * @param phone optional contact phone
     *
     * @throws IllegalArgumentException If the email is null, blank or empty
     */
    public Profile(String name, String email, String phone) {
        this(email);
        this.name = name;
        this.phone = phone;
    }

    /**
     * Copy constructor for this profile
     *
     * @param other The other profile to copy into this profile
     */
    public Profile(Profile other) {
        this.name = other.getName();
        this.email = other.getEmail();
        this.phone = other.getPhone();
        this.deviceId = other.getDeviceId();
        this.type = other.type;
    }

    /**
     * @return profile's display name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the profile's display name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return entrant email contact
     */
    public String getEmail() {
        return email;
    }

    /**
     * Updates the entrant's contact email.
     *
     * @param email New email to update
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return entrant phone contact (optional)
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Updates the entrant's contact phone.
     *
     * @param phone New phone number to add
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return device fingerprint used for passwordless identification
     */
    public ArrayList<String> getDeviceId() {
        return new ArrayList<String>(deviceId);
    }

    /**
     * Add the device fingerprint for this entrant if it has not yet existed
     *
     * @param deviceId
     */
    public void addDeviceId(String deviceId) {
        if (!this.deviceId.contains(deviceId))
            this.deviceId.add(deviceId);
    }

    /**
     * Remove a device Id from this profile
     *
     * @param deviceId The device Id to be removed
     */
    public void removeDeviceId(String deviceId) {
        this.deviceId.remove(deviceId);
    }

    /**
     * @return The type of profile (Admin, Organizer, or Entrant)
     * @see Entrant
     */
    public String getType() {
        return this.type;
    }
}
