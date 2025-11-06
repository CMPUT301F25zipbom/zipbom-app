package com.example.code_zombom_app.Entrant.EditProfile;

import static java.lang.Thread.sleep;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.Helpers.Users.Profile;
import com.example.code_zombom_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfileActivity extends AppCompatActivity implements TView<EditProfileModel> {
    private String email;
    private TextView editName;
    private TextView editEmail;
    private TextView editPhone;
    private ToggleButton toggleNotification;
    private ToggleButton toggleLinkDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_profile);
        email = getIntent().getStringExtra("Email");

        editName = findViewById(R.id.textViewEntrantProfileName);
        editEmail = findViewById(R.id.textViewEntrantProfileEmail);
        editPhone = findViewById(R.id.textViewEntrantProfilePhone);

        toggleNotification = findViewById(R.id.toggleButtonEntrantProfileNotification);
        toggleLinkDevice = findViewById(R.id.toggleButtonEntrantProfileLinkUnlinkDevice);

        EditProfileModel model = new EditProfileModel(FirebaseFirestore.getInstance(), email);
        EditProfileController controller = new EditProfileController(model,
                findViewById(R.id.imageButtonEntrantProfileName),
                findViewById(R.id.imageButtonEntrantProfileEmail),
                findViewById(R.id.imageButtonEntrantProfilePhone),
                editName, editEmail, editPhone,
                findViewById(R.id.buttonEntrantProfileBack),
                findViewById(R.id.buttonEntrantProfileSave),
                findViewById(R.id.buttonEntrantProfileDelete),
                toggleNotification, toggleLinkDevice);
        model.addView(this);
    }

    @Override
    public void update(EditProfileModel model) {
        if (model.getState() == GModel.State.CLOSE)
            finish(); // Close the activity
        else if (model.getState() == GModel.State.REQUEST) {
            if (model.getEditState() == EditProfileModel.EditProfileRequest.POPUP_EDIT) {
                showInputDialog((EditProfileModel.PopUpEdit) model.getInterMsg("Request"),
                        model);
            }
        }
        else if (model.getState() == GModel.State.EDIT_PROFILE_SUCCESS) {
            Entrant newEntrant = (Entrant) model.getInterMsg("Profile");
            Toast.makeText(this, "Successfully make changes to " + newEntrant.getEmail(),
                    Toast.LENGTH_SHORT).show();
            finish(); // Go back to the main activity
        }
        else if (model.getState() == GModel.State.EDIT_PROFILE_FAILURE) {
            Toast.makeText(this, "Cannot edit " + model.getEmail() + " " +
                    model.getErrorMsg(), Toast.LENGTH_SHORT).show();
        }
        else if (model.getState() == GModel.State.DELETE_PROFILE_SUCCESS) {
            Toast.makeText(this, model.getEmail() + " deleted successfully. Process " +
                    "stop in 5 seconds", Toast.LENGTH_LONG).show();

            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Reset the app after 5 seconds
                finishAffinity();
                System.exit(0);
            }, 5000);
        }
        else if (model.getState() == GModel.State.DELETE_PROFILE_FAILURE) {
            Toast.makeText(this, model.getEmail() + " deletion failure: " +
                    model.getErrorMsg(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a popup window to modified the name, or the email, or the phone number
     *
     * @param window Specified how to create the popup window
     * @param model  The model that observing this view
     */
    private void showInputDialog(EditProfileModel.PopUpEdit window, EditProfileModel model) {
        final EditText input = new EditText(this);
        input.setHint(window.getRequest());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(window.getTitle())
                .setMessage(window.getRequest())
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newInput = input.getText().toString().trim();

                        if (newInput.isEmpty()) {
                            Toast.makeText(getApplicationContext(), window.getType() + " cannot be " +
                                    "null or empty", Toast.LENGTH_SHORT).show();
                        } else {
                            switch (window.getType()) {
                                case "Name":
                                    editName.setText(newInput);
                                    model.editName(newInput);
                                    break;
                                case "Email":
                                    editEmail.setText(newInput);
                                    model.editEmail(newInput);
                                    break;
                                case "Phone":
                                    editPhone.setText(newInput);
                                    model.editEmail(newInput);
                                    break;
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
    }
}
