package com.example.code_zombom_app.Helpers.Event;

import android.util.Log;

import com.example.code_zombom_app.Helpers.MVC.GModel;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * <p>
 * A class model that provide all service to an event, such as adding/deleting an event from the
 * database, allowing the users to join the waiting list, etc. <br><br>
 *
 * If you want to use this model, just makes your class implement TView<EventModel> then
 * in your code, add the lines: EventModel model = new EvenModel(); model.add(this);
 * </p>
 *
 * @author Dang Nguyen
 * @version 11/24/2025
 */
public class EventModel extends GModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Event loadedEvent; // The loaded event from the database

    /**
     * Upload an event onto the database.
     *
     * @param event The event to be uploaded to the database
     * @throws IllegalArgumentException If {@code event} is null
     * @see Event
     * @see FirebaseFirestore
     */
    public void uploadEvent(Event event) {
        resetState();

        if (event == null)
            throw new IllegalArgumentException("Event cannot be null");

        db.collection("Events")
                .document(event.getEventId())
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    setState(State.UPLOAD_EVENT_SUCESS);
                    notifyViews();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseFirestore Error", "Cannot add the event to the database",
                            e);
                    setState(State.UPLOAD_EVENT_FAILURE);
                    errorMsg = "Cannot add the event to the database";
                    notifyViews();
                });
    }

    /**
     * Load an event from the database using the event's id.
     *
     * @param id The event's id
     * @see Event
     * @see FirebaseFirestore
     */
    public void loadEvent(String id) {
        db.collection("Events")
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        loadedEvent = documentSnapshot.toObject(Event.class);
                        setState(State.LOAD_EVENT_SUCCESS);
                        notifyViews();
                    }
                    else {
                        setState(State.LOAD_EVENT_FAILURE);
                        errorMsg = "Cannot find the event in the database";
                        notifyViews();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseFirestore", "Cannot query the event", e);
                    setState(State.LOAD_EVENT_FAILURE);
                    notifyViews();
                });

    }

    /**
     * @return The loaded event from the database.
     */
    public Event getLoadedEvent() {
        return loadedEvent;
    }

    protected void resetState() {
        super.resetState();
        loadedEvent = null;
    }
}
