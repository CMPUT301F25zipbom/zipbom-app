package com.example.code_zombom_app;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Represent a profile. Store the personal information such as email addresses, name, phone number,
 * personal identifier and device ID.
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 */
public abstract class Profile {
    protected String name;
    protected String email;
    protected String phone;
    protected String deviceId;
    protected String type; // Admin, organizer or entrant

    /**
     * ALWAYS call this constructor. A profile MUST always be associated with an email address
     */
    public Profile(String email) {
        this.email = email;
    }

    /**
     * Convenient constructor for quickly instantiating an entrant profile.
     *
     * @param name  entrant display name
     * @param email contact email
     * @param phone optional contact phone
     */
    public Profile(String name, String email, String phone) {
        this(email);
        this.name = name;
        this.phone = phone;
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
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Persists the device fingerprint for this entrant.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * @return The type of profile (Admin, Organizer, or Entrant)
     * @see Entrant
     */
    public String getType() {
        return this.type;
    }
}
