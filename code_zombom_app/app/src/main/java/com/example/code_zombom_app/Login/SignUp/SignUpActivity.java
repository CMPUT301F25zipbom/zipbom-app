package com.example.code_zombom_app.Login.SignUp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.MVC.GModel;
import com.example.code_zombom_app.MVC.TView;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity implements TView<SignUpModel>  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        SignUpModel model = new SignUpModel(FirebaseFirestore.getInstance());
        SignUpController controller = new SignUpController(model, this,
                findViewById(R.id.editTextSignUpName),
                findViewById(R.id.editTextSignUpEmail),
                findViewById(R.id.editTextSignUpPhone),
                findViewById(R.id.buttonSignUpBack),
                findViewById(R.id.buttonSignUpSignUp),
                findViewById(R.id.spinnerSignUpUserType));

        model.addView(this);
    }

    @Override
    public void update(SignUpModel model) {
        if (model.getState() == GModel.State.SIGNUP_SUCCESS) {
            Toast.makeText(this, "Sign up succeed!", Toast.LENGTH_SHORT).show();
            finish();
        } else if (model.getState() == GModel.State.SIGNUP_FAILURE) {
            Toast.makeText(this, "Sign Up failed: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
