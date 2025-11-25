package com.example.code_zombom_app.Entrant;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventListAdapter;
import com.example.code_zombom_app.Helpers.MVC.GController;

import java.util.ArrayList;

public class EntrantMainController extends GController<EntrantMainModel> {
    private ImageButton imageButtonFilter;
    private final ImageButton imageButtonProfile;
    private ImageButton imageButtonCamera;
    private ListView listViewEvents;
    private EventListAdapter eventListAdapter;

    public EntrantMainController(EntrantMainModel M,
                                 ImageButton filter, ImageButton profile, ImageButton camera,
                                 ListView events, EventListAdapter adapter) {
        super(M);
        imageButtonFilter = filter;
        imageButtonProfile = profile;
        imageButtonCamera = camera;
        listViewEvents = events;
        eventListAdapter = adapter;
    }

    @Override
    public void bindView() {
        imageButtonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EntrantMainModel) model).open("Profile");
            }
        });
    }
}
