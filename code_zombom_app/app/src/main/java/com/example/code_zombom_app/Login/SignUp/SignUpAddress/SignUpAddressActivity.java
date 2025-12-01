package com.example.code_zombom_app.Login.SignUp.SignUpAddress;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Login.SignUp.SignUpModel;
import com.example.code_zombom_app.R;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;

public class SignUpAddressActivity extends AppCompatActivity implements TView<SignUpModel> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.entrant_activity_signup_address);

        if (!Places.isInitialized())
            Places.initialize(getApplicationContext(), Location.getGoogleApi());

        SignUpModel model = (SignUpModel) GModel.getCurrentModel();

        SignUpAddressController controller = new SignUpAddressController(model,
                (AutocompleteSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_autoComplete_address),
                findViewById(R.id.buttonBackFromActivitySignUpAddress),
                findViewById(R.id.buttonEnterSignUpAddress));
        controller.bindView();

        model.addView(this);
    }

    @Override
    public void update(SignUpModel model) {
        if (model.getState() == GModel.State.CLOSE ||
                model.getState() == GModel.State.SIGNUP_ADDRESS_SUCCESS) {
            model.deleteView(this);
            finish();
        }
        else if (model.getState() == GModel.State.SIGNUP_ADDRESS_FAILURE) {
            Toast.makeText(this, "Sign for this address failed: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
