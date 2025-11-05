package com.example.code_zombom_app.Login.SignUp;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.code_zombom_app.MVC.GController;
import com.example.code_zombom_app.R;

public class SignUpController extends GController<SignUpModel> {
    private final EditText editTextName;
    private final EditText editTextEmail;
    private final EditText editTextPhone;
    private final Button buttonBack;
    private final Button buttonSignUp;
    private final ArrayAdapter<CharSequence> userTypeAdapter;
    private final Spinner spinnerUserType;
    private String selectedType;
    private String Name;
    private String Email;
    private String Phone;

    public SignUpController(SignUpModel M, Context context,
                            EditText name, EditText email, EditText phone, Button back,
                            Button signUp, Spinner userType)
    {
        super(M, context);
        editTextName = name;
        editTextEmail = email;
        editTextPhone = phone;
        buttonBack = back;
        buttonSignUp = signUp;
        spinnerUserType = userType;

        userTypeAdapter = ArrayAdapter.createFromResource(
                this.context,
                R.array.user_types,
                android.R.layout.simple_spinner_item
        );
        userTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(userTypeAdapter);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedType = "Entrant";
            }
        });

        editTextName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Name = editTextName.getText().toString().trim();
                    /* Collapse the keyboard after done */
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        editTextEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Email = editTextEmail.getText().toString().trim();
                    /* Collapse the keyboard after done */
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        editTextPhone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Phone = editTextPhone.getText().toString().trim();
                    /* Collapse the keyboard after done */
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SignUpModel) model).setProfile(Name, Email, Phone, selectedType);
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((android.app.Activity) context).finish();
            }
        });
    }
}
