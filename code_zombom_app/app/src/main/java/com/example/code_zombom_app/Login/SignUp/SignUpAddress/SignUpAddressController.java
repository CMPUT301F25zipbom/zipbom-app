package com.example.code_zombom_app.Login.SignUp.SignUpAddress;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.example.code_zombom_app.Helpers.MVC.GController;
import com.example.code_zombom_app.Login.SignUp.SignUpModel;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;

public class SignUpAddressController extends GController<SignUpModel> {
    private AutocompleteSupportFragment autocompleteSupportFragmentAddress;
    private Button buttonBackFromSignUpAddressActivity;
    private Button buttonSignUpAddress;
    private Place selected;

    public SignUpAddressController(SignUpModel M,
                                   AutocompleteSupportFragment auto,
                                   Button back, Button enter) {
        super(M);
        this.autocompleteSupportFragmentAddress = auto;
        this.buttonBackFromSignUpAddressActivity = back;
        this.buttonSignUpAddress = enter;
    }

    @Override
    public void bindView() {
        bindAutoFillAddress();

        buttonBackFromSignUpAddressActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.close();
            }
        });

        buttonSignUpAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.setLocation(selected);
            }
        });
    }

    private void bindAutoFillAddress() {
        assert autocompleteSupportFragmentAddress != null;
        autocompleteSupportFragmentAddress.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG));
        autocompleteSupportFragmentAddress.setHint("Enter your address");
        autocompleteSupportFragmentAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.e("Address", "Error: " + status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.i("Address", "Selected: " + place.getAddress());
                Log.i("Location", "LatLng: " + place.getLatLng());
                selected = place;
            }
        });
    }
}
