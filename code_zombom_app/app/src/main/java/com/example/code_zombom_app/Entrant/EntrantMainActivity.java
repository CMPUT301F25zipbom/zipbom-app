package com.example.code_zombom_app.Entrant;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.R;

public class EntrantMainActivity extends AppCompatActivity implements TView<EntrantMainModel> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_main);
    }

    @Override
    public void update(EntrantMainModel model) {

    }
}
