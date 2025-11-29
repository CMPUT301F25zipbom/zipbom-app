package com.example.code_zombom_app.Helpers.Location;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.code_zombom_app.Helpers.Event.Event;

/**
 * An activity that launch a heat map of the location of entrant that joined the waiting list
 * of an event
 *
 * @author Dang Nguyen
 * @version 11/28/2025
 * @see com.google.android.gms.maps.GoogleMap
 * @see com.example.code_zombom_app.Helpers.Users.Entrant
 * @see com.example.code_zombom_app.Helpers.Event.Event
 * @see Location
 */
public class EventHeatMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String EXTRA_EVENT_ID = "eventId";
    private GoogleMap googleMap;
    private TileOverlay heatmapOverlay;
    private FirebaseFirestore db;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_heatmap);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.heatmap_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("EventHeatMapActivity", "SupportMapFragment is null");
            Toast.makeText(this, "Unable to load map", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        loadEventAndDrawHeatMap();
    }

    private void loadEventAndDrawHeatMap() {
        db.collection("Events") // adjust collection name if needed
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Event not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Event event = snapshot.toObject(Event.class);
                    if (event == null) {
                        Toast.makeText(this, "Failed to parse event",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    fetchWaitingEntrantLocations(event);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventHeatMapActivity", "Failed to load event", e);
                    Toast.makeText(this, "Failed to load event",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fetchWaitingEntrantLocations(Event event) {
        ArrayList<String> waitingList = event.getWaitingList();

        if (waitingList == null || waitingList.isEmpty()) {
            Toast.makeText(this,
                    "No entrants on the waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference profilesRef = db.collection("Profiles");
        ArrayList<Location> locations = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(waitingList.size());
        AtomicBoolean errorOccurred = new AtomicBoolean(false);

        for (String email : waitingList) {
            if (email == null || email.isEmpty()) {
                if (remaining.decrementAndGet() == 0 && !errorOccurred.get()) {
                    drawHeatMap(locations);
                }
                continue;
            }

            profilesRef.document(email)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            Entrant entrant = snapshot.toObject(Entrant.class);
                            if (entrant != null && entrant.getLocation() != null) {
                                locations.add(entrant.getLocation());
                            }
                        }

                        if (remaining.decrementAndGet() == 0 && !errorOccurred.get()) {
                            drawHeatMap(locations);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventHeatMapActivity",
                                "Failed to load entrant " + email, e);
                        errorOccurred.set(true);

                        if (remaining.decrementAndGet() == 0) {
                            Toast.makeText(this,
                                    "Some entrant locations failed to load",
                                    Toast.LENGTH_SHORT).show();
                            drawHeatMap(locations);
                        }
                    });
        }
    }

    private void drawHeatMap(ArrayList<Location> locations) {
        if (googleMap == null) return;

        if (locations.isEmpty()) {
            Toast.makeText(this,
                    "No entrant locations to display", Toast.LENGTH_SHORT).show();
            return;
        }

        if (heatmapOverlay != null) {
            heatmapOverlay.remove();
        }

        heatmapOverlay = Location.generateHeatMap(googleMap, locations);
    }
}
