package com.example.code_zombom_app.Login.SignUp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Login.SignUp.SignUpAddress.SignUpAddressActivity;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity implements TView<SignUpModel>  {
    private TextView textViewAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        SignUpModel model = new SignUpModel(FirebaseFirestore.getInstance());
        GModel.setCurrentModel(model);
        SignUpController controller = new SignUpController(model,
                findViewById(R.id.editTextSignUpName),
                findViewById(R.id.editTextSignUpEmail),
                findViewById(R.id.editTextSignUpPhone),
                findViewById(R.id.buttonSignUpBack),
                findViewById(R.id.buttonSignUpSignUp),
                findViewById(R.id.buttonSignUpAddress),
                findViewById(R.id.spinnerSignUpUserType),
                ArrayAdapter.createFromResource(
                        this,
                        R.array.user_types,
                        android.R.layout.simple_spinner_item
                ));
        controller.bindView();

        model.addView(this);

        textViewAddress = findViewById(R.id.textViewAddress);
    }

    @Override
    public void update(SignUpModel model) {
        textViewAddress.setText("");

        if (model.getState() == GModel.State.SIGNUP_SUCCESS) {
            Toast.makeText(this, "Sign up succeed!", Toast.LENGTH_SHORT).show();
            finishAndCleanUp();

        } else if (model.getState() == GModel.State.SIGNUP_FAILURE) {
            Toast.makeText(this, "Sign Up failed: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        } else if (model.getState() == GModel.State.CLOSE) {
            finishAndCleanUp();
        } else if (model.getState() == GModel.State.OPEN) {
            Object extra = model.getInterMsg("Extra");
            if (extra instanceof String) {
                if (extra.equals("Address")) {
                    Intent signUpAddress = new Intent(this, SignUpAddressActivity.class);
                    startActivity(signUpAddress);
                }
            }
        } else if (model.getState() == GModel.State.SIGNUP_ADDRESS_SUCCESS) {
            Location location = (Location) model.getInterMsg("Message");
            textViewAddress.setText(location.toString());
        }
    }

    /**
     * Helper method to close this activity and clean up
     */
    private void finishAndCleanUp() {
        // Manually delete current using model
        GModel.resetCurrentModel();
        finish();
    }
}
