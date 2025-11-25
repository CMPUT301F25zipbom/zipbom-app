package com.example.code_zombom_app.Entrant;

import android.util.Log;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventModel;
import com.example.code_zombom_app.Helpers.Event.EventService;
import com.example.code_zombom_app.Helpers.Filter.EventFilter;
import com.example.code_zombom_app.Helpers.MVC.GModel;
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
    public EntrantMainModel() {
        super();
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
                        Event event = doc.toObject(Event.class);
                        if (filter.passFilter(event))
                            loadedEvents.add(event);
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
}
