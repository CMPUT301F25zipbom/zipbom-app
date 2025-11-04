package com.example.code_zombom_app;

import com.example.code_zombom_app.MVC.TModel;
import com.example.code_zombom_app.MVC.TView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * The idea for now is this is the MAIN model ("brain") of our project. Every other activity should
 * be a "view" or "controller"
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/4/2025
 * @see com.example.code_zombom_app.MVC.TModel
 */
public class MainModel extends TModel<TView> {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void createProfile(Entrant entrant) {

    }
}
