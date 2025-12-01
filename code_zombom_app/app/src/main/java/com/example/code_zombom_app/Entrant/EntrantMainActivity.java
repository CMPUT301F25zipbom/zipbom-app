package com.example.code_zombom_app.Entrant;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.code_zombom_app.Entrant.EditProfile.EditProfileActivity;
//import com.example.code_zombom_app.EntrantEventListViewModel;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventListAdapter;
import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.Helpers.Filter.EventFilter;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.EventForOrg;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EntrantMainActivity extends AppCompatActivity implements TView<EntrantMainModel> {
    private String email;
    //private EntrantEventListViewModel eventViewModel;
    private ActivityResultLauncher<Intent> filterLauncher;
    private EventListAdapter eventListAdapter;
    private ArrayList<Event> events;
    private ListView listViewEvent;

    private boolean isActive = false;
    private AlertDialog qrDialog;

    private Entrant entrant;
    private ListenerRegistration notificationListener;
    private boolean notificationsEnabled = true;

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
        loadNotificationPreferenceAndStartListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (qrDialog != null && qrDialog.isShowing()) {
            qrDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.entrant_main_activity);

        email = getIntent().getStringExtra("Email"); // Get the email address

        events = new ArrayList<>();
        eventListAdapter = new EventListAdapter(this, events, email);
        listViewEvent = findViewById(R.id.listViewEntrantEvent);
        listViewEvent.setAdapter(eventListAdapter);

        EntrantMainModel model = new EntrantMainModel(email);


        ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
                new ScanContract(),
                result -> {
                    if (result == null || result.getContents() == null) {
                        Toast.makeText(this, "No QR content detected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String scannedEventId = result.getContents();
                    android.util.Log.d("QR_SCAN", "Scanned id = " + scannedEventId);

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("Events")
                            .document(scannedEventId)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (!isActive)
                                    return;

                                if (!snapshot.exists()) {
                                    Toast.makeText(this, "Invalid QR code (no such event)", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Event event = snapshot.toObject(Event.class);
                                if (event == null) {
                                    Toast.makeText(this, "Error loading event from QR", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Show the popup with full functionality
                                openEventPopUpFromEvent(event);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("QR_SCAN", "Failed to load event by QR", e);
                                Toast.makeText(this, "Error loading event for QR code", Toast.LENGTH_SHORT).show();
                            });
                }
        );

        EntrantMainController controller = new EntrantMainController(model,
                findViewById(R.id.imageButtonFilter),
                findViewById(R.id.imageButtonProfile),
                findViewById(R.id.imageButtonCamera),
                listViewEvent,
                eventListAdapter,
                barcodeLauncher
        );

        controller.bindView();
        model.addView(this);
        model.loadEvents();

        listViewEvent.setOnItemClickListener((parent, view,
                                              position, id) -> {
            Event event = events.get(position);
            if (event != null) {
                openEventPopUpFromEvent(event);
            }
        });
    }

//    private void showFilterActivity() {
//        Intent intent = new Intent(this, FilterSortActivity.class);
//        intent.putExtra(FilterSortActivity.EXTRA_INITIAL_STATE, eventViewModel.getFilterSortState());
//        filterLauncher.launch(intent);
//    }

    @Override
    public void update(EntrantMainModel model) {
        Object extra = model.getInterMsg("Extra");
        if (model.getState() == GModel.State.OPEN) {
            if (extra instanceof String) {
                if ("Profile".equals(extra)) {
                    Intent editProfile = new Intent(this, EditProfileActivity.class);
                    editProfile.putExtra("Email", email);
                    startActivity(editProfile);
                }
            }
        }
        else if (model.getState() == GModel.State.LOAD_EVENTS_SUCCESS) {
            events.clear();
            events.addAll(model.getLoadedEvents());

            android.util.Log.d("EVENT_LOAD", "Loaded " +
                    events.size() + " events into adapter");

            eventListAdapter.notifyDataSetChanged();
        }
        else if (model.getState() == GModel.State.LOAD_EVENTS_FAILURE) {
            Toast.makeText(this, "Error in loading the events: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        }
        else if (model.getState() == GModel.State.REQUEST_FILTER_EVENT) {
            openFilterPopUpWindow(model);
        }
    }

    /**
     * Open a popup window that allow the users to enter options for filtering the events
     *
     * @param model The control model of this view
     */
    private void openFilterPopUpWindow(EntrantMainModel model) {
        EventFilter filter = new EventFilter();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.entrant_filter_events_popup, null);

        Spinner spinnerGenre = view.findViewById(R.id.spinnerFilterByGenre);

        String[] genresString = Event.getAcceptedCategories();
        List<String> genres = new ArrayList<>(Arrays.asList(genresString));
        genres.add(0, "Any");

        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                genres);

        spinnerGenre.setAdapter(genreAdapter);

        CheckBox checkBoxAvailability = view.findViewById(R.id.checkBox_filter_by_availability);
        LinearLayout linearLayoutAvailability = view.findViewById(
                R.id.linearLayout_filter_by_availability);

        checkBoxAvailability.setOnCheckedChangeListener((b, checked) -> {
            linearLayoutAvailability.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        Calendar today = Calendar.getInstance();
        Calendar nextDay = Calendar.getInstance();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);

        DatePicker datePickerStartDate = view.findViewById(
                R.id.datePicker_filter_by_availability_startDate);
        DatePicker datePickerEndDate = view.findViewById(
                R.id.datePicker_filter_by_availability_endDate);

        datePickerStartDate.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH));
        datePickerEndDate.updateDate(nextDay.get(Calendar.YEAR), nextDay.get(Calendar.MONTH),
                nextDay.get(Calendar.DAY_OF_MONTH));

        /* Preventing setting the end date to be earlier than the start date and the start date
         * to be earlier than today
         */
        datePickerStartDate.setMinDate(today.getTimeInMillis());
        datePickerEndDate.setMinDate(nextDay.getTimeInMillis());

        /* Automatically update the chosen dates when the users enter an invalid date */
        datePickerStartDate.init(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
                (datePicker, year, month, day) -> {

                    Calendar start = Calendar.getInstance();
                    start.set(year, month, day);

                    // End date must be at least 1 day after
                    Calendar minEnd = (Calendar) start.clone();
                    minEnd.add(Calendar.DAY_OF_MONTH, 1);

                    datePickerEndDate.setMinDate(minEnd.getTimeInMillis());

                    // If current end < new minEnd -> reset
                    Calendar currentEnd = Calendar.getInstance();
                    currentEnd.set(
                            datePickerEndDate.getYear(),
                            datePickerEndDate.getMonth(),
                            datePickerEndDate.getDayOfMonth()
                    );

                    if (currentEnd.before(minEnd)) {
                        datePickerEndDate.updateDate(
                                minEnd.get(Calendar.YEAR),
                                minEnd.get(Calendar.MONTH),
                                minEnd.get(Calendar.DAY_OF_MONTH)
                        );
                    }
                }
        );

        datePickerEndDate.init(
                nextDay.get(Calendar.YEAR),
                nextDay.get(Calendar.MONTH),
                nextDay.get(Calendar.DAY_OF_MONTH),
                (datePicker, year, month, day) -> {

                    Calendar start = Calendar.getInstance();
                    start.set(
                            datePickerStartDate.getYear(),
                            datePickerStartDate.getMonth(),
                            datePickerStartDate.getDayOfMonth()
                    );

                    Calendar selectedEnd = Calendar.getInstance();
                    selectedEnd.set(year, month, day);

                    // Minimum valid end: start + 1 day
                    Calendar minEnd = (Calendar) start.clone();
                    minEnd.add(Calendar.DAY_OF_MONTH, 1);

                    // If user selects an invalid end date -> automatically correct it
                    if (selectedEnd.before(minEnd)) {
                        datePickerEndDate.updateDate(
                                minEnd.get(Calendar.YEAR),
                                minEnd.get(Calendar.MONTH),
                                minEnd.get(Calendar.DAY_OF_MONTH)
                        );
                    }
                });

        Button buttonReset = view.findViewById(R.id.button_filter_event_reset);
        Button buttonApply = view.findViewById(R.id.button_filter_event_apply);

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter.reset();

                // Set the spinner back to position zero
                spinnerGenre.setSelection(0);

                checkBoxAvailability.setChecked(false);
                linearLayoutAvailability.setVisibility(View.GONE);

                datePickerStartDate.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH));
                datePickerEndDate.updateDate(nextDay.get(Calendar.YEAR), nextDay.get(Calendar.MONTH),
                        nextDay.get(Calendar.DAY_OF_MONTH));

                model.loadEvents();

                dialog.dismiss();
            }
        });

        buttonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedGenre = (String) spinnerGenre.getSelectedItem();
                if (selectedGenre != null && !selectedGenre.equals("Any")) {
                    filter.setFilterGenre(selectedGenre);
                } else {
                    filter.setFilterGenre(null);
                }

                if (checkBoxAvailability.isChecked()) {
                    Date startDate = getDateFromDatePicker(datePickerStartDate);
                    Date endDate = getDateFromDatePicker(datePickerEndDate);

                    filter.setFilterStartDate(startDate);
                    filter.setFilterEndDate(endDate);
                } else {
                    filter.setFilterStartDate(null);
                    filter.setFilterEndDate(null);
                }

                model.filterEvent(filter);
                dialog.dismiss();
            }
        });

    }

    /**
     * Get a Date from the DatePicker class.
     *
     * @param datePicker The DatePicker object, which contains the date the the users
     *                   have selected
     * @return A Date that have been selected by the users
     */
    private Date getDateFromDatePicker(DatePicker datePicker) {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();            // 0-based (January = 0)
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.set(year, month, day);

        return calendar.getTime();
    }

    /**
     * Open a pop up window that shows a single Event (used by QR scan).
     */
    private void openEventPopUpFromEvent(Event event) {
        // Extra guard
        if (!isActive) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.entrant_full_event_details, null, false);

        // Map Event -> EventForOrg (like you did in adapter)
        EventForOrg dto = EventMapper.toDto(event);

        ImageView posterImageView = view.findViewById(R.id.imageView_entrant_full_details_poster);
        TextView nameValue = view.findViewById(R.id.textView_entrant_event_full_details_name);
        TextView dateValue = view.findViewById(R.id.textView_entrant_event_full_details_startDate);
        TextView deadlineValue = view.findViewById(R.id.textView_entrant_event_full_details_endDate);
        TextView locationValue = view.findViewById(R.id.textView_entrant_event_full_details_location);
        TextView genreValue = view.findViewById(R.id.textView_entrant_event_full_details_genre);
        TextView maxPeopleValue = view.findViewById(
                R.id.textView_entrant_event_full_details_maxPeople);
        TextView waitlistMaxValue = view.findViewById(
                R.id.textView_entrant_event_full_details_maxWaitlist);
        TextView descriptionValue = view.findViewById(
                R.id.textView_entrant_event_full_details_description);
        Button join = view.findViewById(R.id.button_entrant_event_full_details_joinWaitingList);
        Button leave = view.findViewById(R.id.button_entrant_event_full_details_leaveWaitingList);

        EventService eventService = new EventService();

        if (event != null && event.isInWaitingList(email)) {
            join.setEnabled(false);
            leave.setEnabled(true);
        } else {
            boolean alreadySelected = event != null && (event.getChosenList().contains(email)
                    || event.getPendingList().contains(email)
                    || event.getRegisteredList().contains(email));
            join.setEnabled(!alreadySelected);
            leave.setEnabled(false);
        }

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    assert event != null;
                    if (event.getChosenList().contains(email)
                            || event.getPendingList().contains(email)
                            || event.getRegisteredList().contains(email)) {
                        Toast.makeText(v.getContext(),
                                "You have already been selected for this event.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    event.joinWaitingList(email);
                    for (Event e : events) {
                        if (e.getEventId().equals(event.getEventId())) {
                            e.joinWaitingList(email);
                            break;
                        }
                    }
                    eventService.addEntrantToWaitlist(event.getEventId(), email);
                    leave.setEnabled(true);
                    join.setEnabled(false);
                    eventListAdapter.notifyDataSetChanged();
                    Toast.makeText(v.getContext(), "Join Waiting list successfully",
                            Toast.LENGTH_SHORT).show();
                } catch (RuntimeException e) {
                    Log.e("Join Event Error", "Waiting list is full", e);
                    Toast.makeText(v.getContext(), "This event waiting list is full!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert event != null;
                event.leaveWaitingList(email);
                eventService.removeEntrantFromWaitlist(event.getEventId(), email);
                for (Event e : events) {
                    if (e.getEventId().equals(event.getEventId())) {
                        e.leaveWaitingList(email);
                        break;
                    }
                }
                leave.setEnabled(false);
                join.setEnabled(true);
                Toast.makeText(v.getContext(), "Leave waiting list successfully",
                        Toast.LENGTH_SHORT).show();
                eventListAdapter.notifyDataSetChanged();
            }
        });

        // Fill in values from dto
        nameValue.setText(dto.getName());
        dateValue.setText(dto.getDate());
        deadlineValue.setText(dto.getDeadline());
        locationValue.setText(dto.getLocation() != null
                ? dto.getLocation().toString()
                : "-");
        genreValue.setText(dto.getGenre());
        maxPeopleValue.setText(dto.getMax_People());
        waitlistMaxValue.setText(dto.getWait_List_Maximum());
        descriptionValue.setText(dto.getDescription());

        if (dto.getPosterUrl() != null && !dto.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(dto.getPosterUrl())
                    .into(posterImageView);
            posterImageView.setVisibility(View.VISIBLE);
        } else {
            posterImageView.setVisibility(View.GONE);
        }

        // Close previous dialog if any
        if (qrDialog != null && qrDialog.isShowing()) {
            qrDialog.dismiss();
        }

        qrDialog = new AlertDialog.Builder(this)
                .setTitle("Event")
                .setView(view)
                .setNegativeButton("Close", (d, which) -> d.dismiss())
                .create();

        qrDialog.show();
    }

    private void loadNotificationPreferenceAndStartListener() {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("Profiles")
                .document(email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Boolean enabled = snapshot.getBoolean("notificationEnabled");
                    notificationsEnabled = enabled == null || enabled;
                    if (notificationsEnabled) {
                        startNotificationListener();
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, default to enabled so entrants still get critical updates
                    notificationsEnabled = true;
                    startNotificationListener();
                });
    }

    private void startNotificationListener() {
        if (notificationListener != null || email == null || email.trim().isEmpty()) {
            return;
        }
        notificationListener = FirebaseFirestore.getInstance()
                .collectionGroup("Notifications")
                .whereEqualTo("recipientEmail", email.trim().toLowerCase())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null || snap.isEmpty()) {
                        return;
                    }
                    snap.getDocuments().forEach(doc -> {
                        Boolean seen = doc.getBoolean("seen");
                        if (seen != null && seen) {
                            return;
                        }
                        String type = doc.getString("type");
                        String eventName = doc.getString("eventName");
                        String message = doc.getString("message");
                        showInAppNotification(eventName, message, type);
                        doc.getReference().update("seen", true);
                    });
                });
    }

    private void showInAppNotification(String eventName, String message, String type) {
        if (!isActive) {
            return;
        }
        String title = (eventName == null || eventName.trim().isEmpty())
                ? "Notification"
                : eventName;
        String body = (message == null || message.trim().isEmpty())
                ? defaultMessage(type, eventName)
                : message;

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .show();
    }

    private String defaultMessage(String type, String eventName) {
        String name = (eventName == null || eventName.trim().isEmpty()) ? "this event" : eventName;
        if ("win".equalsIgnoreCase(type) || "org_selected".equalsIgnoreCase(type)) {
            return "Congratulations! You are a lottery winner and have been selected for " + name;
        } else if ("lose".equalsIgnoreCase(type)) {
            return "You were not selected this time for " + name;
        } else {
            return "Update for " + name;
        }
    }

    // You can reuse this helper from your adapter (or move it here)
    private String formatListToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "None";
        }
        return TextUtils.join(", ", list);
    }

}
