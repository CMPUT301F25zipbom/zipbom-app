package com.example.code_zombom_app.Entrant;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.code_zombom_app.Entrant.EditProfile.EditProfileActivity;
//import com.example.code_zombom_app.EntrantEventListViewModel;
import com.example.code_zombom_app.FilterSortActivity;
import com.example.code_zombom_app.FilterSortState;
import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventListAdapter;
import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.example.code_zombom_app.R;

import java.util.ArrayList;

public class EntrantMainActivity extends AppCompatActivity implements TView<EntrantMainModel> {
    private String email;
    //private EntrantEventListViewModel eventViewModel;
    private ActivityResultLauncher<Intent> filterLauncher;
    private EventListAdapter eventListAdapter;
    private ArrayList<Event> events;
    private ListView listViewEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entrant_main);

        email = getIntent().getStringExtra("Email"); // Get the email address

//        eventViewModel = new ViewModelProvider(this).get(EntrantEventListViewModel.class);
//        // Share the signed-in entrant address so the event list can perform join mutations
//        eventViewModel.setEntrantEmail(email);
//
//        filterLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
//                        FilterSortState state = (FilterSortState) result.getData()
//                                .getSerializableExtra(FilterSortActivity.EXTRA_RESULT_STATE);
//                        if (state != null) {
//                            eventViewModel.setFilterSortState(state);
//                            boolean reset = result.getData()
//                                    .getBooleanExtra(FilterSortActivity.EXTRA_RESULT_RESET, false);
//                            int messageRes = reset
//                                    ? R.string.filter_sort_reset_summary
//                                    : R.string.filter_sort_applied_summary;
//                            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });

//        ImageButton filterButton = findViewById(R.id.imageButtonFilter);
//        if (filterButton != null) {
//            filterButton.setOnClickListener(v -> showFilterActivity());
//        }

        EntrantMainModel model = new EntrantMainModel();
        events = model.getLoadedEvents();
        eventListAdapter = new EventListAdapter(this, events);
        listViewEvent = findViewById(R.id.listViewEntrantEvent);
        listViewEvent.setAdapter(eventListAdapter);

        EntrantMainController controller = new EntrantMainController(model,
                findViewById(R.id.imageButtonFilter),
                findViewById(R.id.imageButtonProfile),
                findViewById(R.id.imageButtonCamera),
                listViewEvent,
                eventListAdapter

        );

        controller.bindView();
        model.addView(this);
        model.loadEvents();
        events = model.getLoadedEvents();

        int i = 1; // An entry point for debugging
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
                eventListAdapter.notifyDataSetChanged();
        }
        else if (model.getState() == GModel.State.LOAD_EVENTS_FAILURE) {
            Toast.makeText(this, "Error in loading the events: " + model.getErrorMsg(),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
