package com.example.code_zombom_app.Login.SignUp;

import android.util.Log;

import com.example.code_zombom_app.Helpers.Location.Coordinate;
import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.google.android.libraries.places.api.model.Place;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpModel extends LoadUploadProfileModel {

    public SignUpModel(FirebaseFirestore db) {
        super(db);
    }

    /**
     * Return a {@code Location} through the "Message" channel and set the state to
     * SIGNUP_ADDRESS_SUCCESS upon success or SIGNUP_ADDRESS_SUCCESS upon failure
     *
     * @param place The selected place
     */
    public void setLocation(Place place) {
        new Thread(() -> {
            if (place != null && place.getLatLng() != null) {
                Coordinate coordinate = new Coordinate(
                        place.getLatLng().latitude,
                        place.getLatLng().longitude
                );

                Location location = Location.fromCoordinates(coordinate);

                setState(State.SIGNUP_ADDRESS_SUCCESS);
                setInterMsg("Message", location);
            } else {
                Log.w("Location", "No Place selected!");
                setState(State.SIGNUP_ADDRESS_FAILURE);
                errorMsg = "No place has been selected!";
            }

            // Switch to UI thread for notifying views
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::notifyViews);
        }).start();

    }
}
