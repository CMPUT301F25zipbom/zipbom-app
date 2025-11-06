package com.example.code_zombom_app.Helpers.Users;

/**
 * An Entrant class that allows the modification of email address, which makes our life easier
 * for updating the email address in the database
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/5/2025
 * @see Entrant
 */
public class MockUpEntrant extends Entrant {
    private String MockUpEmail;

    public MockUpEntrant(Entrant other) {
        super(other);
        MockUpEmail = other.getEmail();
    }

    public void setEmail(String email) {
        this.MockUpEmail = email;
    }

    @Override
    public String getEmail() {
        return MockUpEmail;
    }
}
