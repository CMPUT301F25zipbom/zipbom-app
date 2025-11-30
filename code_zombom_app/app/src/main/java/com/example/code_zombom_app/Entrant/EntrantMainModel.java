package com.example.code_zombom_app.Entrant;

import android.util.Log;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Models.EventModel;
import com.example.code_zombom_app.Helpers.Filter.EventFilter;
import com.example.code_zombom_app.Helpers.MVC.TView;
import com.example.code_zombom_app.Helpers.Models.LoadUploadProfileModel;
import com.example.code_zombom_app.Helpers.Users.Entrant;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Use this class as the main method to control the main entrant's activity
 *
 * @author Dang Nguyen
 * @version 11/24/2025
 * @see EventModel
 */
public class EntrantMainModel extends EventModel {
    private final String email;
    private Entrant entrant;

    public EntrantMainModel(String email) {
        super();
        this.email = email;
        this.entrant = null;
    }

    //test db
    public EntrantMainModel(String email, FirebaseFirestore firestore) {
        super(firestore);

        this.email = email;
    }

    /**
     * Request the view to open a pop-up window for filtering the events
     */
    public void requestFilter() {
        setState(State.REQUEST_FILTER_EVENT);
        notifyViews();
    }

    /**
     * Filter the events
     *
     * @param filter The applied filter
     * @see com.example.code_zombom_app.Helpers.Filter.EventFilter
     */
    public void filterEvent(EventFilter filter) {
        db.collection("Events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    loadedEvents.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Event event = doc.toObject(Event.class);
                            if (filter.passFilter(event))
                                loadedEvents.add(event);
                        } // Safely-ignored any incompatible Event document for now
                        catch (Exception e) {
                            Log.e("FireStore Error", "Imcompatible documents", e);
                        }
                    }
                    setState(State.LOAD_EVENTS_SUCCESS);
                    notifyViews();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseFirestore Error", "Cannot query the events", e);
                    setState(State.LOAD_EVENTS_FAILURE);
                    errorMsg = "Cannot query the database for the events";
                    notifyViews();
                });
    }

    /**
     * SUPER unprofessional method to load the profile, but IDGAF anymore
     */
    public void loadProfile() {
        class TempView implements TView<LoadUploadProfileModel> {
            @Override
            public void update(LoadUploadProfileModel model) {
                entrant = (Entrant) model.getInterMsg("Profile");
            }
        }

        TempView tempView = new TempView();

        LoadUploadProfileModel profileModel = new LoadUploadProfileModel(
                FirebaseFirestore.getInstance());
        profileModel.addView(tempView);

        setState(State.LOAD_PROFILE_SUCCESS);
        notifyViews();
    }

    /**
     * @return The entrant's profile
     */
    public Entrant getEntrant() {
        return entrant;
    }
}
