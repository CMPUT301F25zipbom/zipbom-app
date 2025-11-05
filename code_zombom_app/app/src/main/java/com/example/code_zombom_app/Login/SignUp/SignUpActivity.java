package com.example.code_zombom_app.Login.SignUp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.MVC.TView;
import com.example.code_zombom_app.R;

public class SignUpActivity extends AppCompatActivity implements TView<LogInModel>  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);
    }
}
