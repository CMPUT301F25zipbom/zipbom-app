package com.example.code_zombom_app;

/**
 * A callback interface to handle asynchronous loading of a {@link Profile} from the database.
 * <p>
 * Implement this interface to receive the result of a profile load operation. The
 * {@link #onProfileLoaded(Profile)} method will be called when the profile has been successfully
 * loaded or if no profile exists (in which case the {@code profile} parameter may be {@code null}).
 * </p>
 *
 * @author chatGpt
 * @version 1.0.0, 11/4/2025
 * @see Profile
 */
public interface ProfileCallBack {
    void onProfileLoaded(Profile profile);
}
