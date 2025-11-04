package com.example.code_zombom_app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class AddEventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private EditText eventNameEditText;
    private EditText maxPeopleEditText;
    private EditText dateEditText;
    private EditText deadlineEditText;
    private EditText genreEditText;
    private EditText locationEditText;
    private CollectionReference eventref;
    private FirebaseFirestore db;
    private CollectionReference events;
    private Button saveEventButton;
    // Create list of events
    ArrayList<ArrayList<String>> listOfEvents = new ArrayList<ArrayList<String>>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Scoped to the Activity, so it's shared between fragments.
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        events = db.collection("Events");

        eventref = db.collection("Events");

        Button cancelButton = view.findViewById(R.id.cancelButton);

        eventNameEditText = view.findViewById(R.id.editTextName);
        maxPeopleEditText = view.findViewById(R.id.editTextMaxPeople);
        dateEditText = view.findViewById(R.id.editTextDate);
        deadlineEditText = view.findViewById(R.id.editTextDeadline);
        genreEditText = view.findViewById(R.id.editTextGenre);
        locationEditText = view.findViewById(R.id.editTextLocation);


        saveEventButton = view.findViewById(R.id.saveEventButton);

        cancelButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
        });

        //TODO: add poster and QR code generation
        saveEventButton.setOnClickListener(v -> {

            String eventName = eventNameEditText.getText().toString();
            if (!eventName.isEmpty() && !maxPeopleEditText.getText().toString().isEmpty()
                    && !dateEditText.getText().toString().isEmpty() && !deadlineEditText.getText().toString().isEmpty()
                    && !genreEditText.getText().toString().isEmpty()) {
                //just for the UI visuals
                String name = eventNameEditText.getText().toString();
                String maxPeople = maxPeopleEditText.getText().toString();
                String date = dateEditText.getText().toString();
                String deadline = deadlineEditText.getText().toString();
                String genre = genreEditText.getText().toString();
                String location = locationEditText.getText().toString();

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("Name", name);
                eventData.put("Max People", maxPeople);
                eventData.put("Date", date);
                eventData.put("Deadline", deadline);
                eventData.put("Genre", genre);
                if(location.isEmpty() == false){
                    eventData.put("Location", location);
                }
                db.collection("Events").add(eventData);

                // Navigate back to the main fragment
                NavHostFragment.findNavController(AddEventFragment.this).navigateUp();
            }
        });
    }

}