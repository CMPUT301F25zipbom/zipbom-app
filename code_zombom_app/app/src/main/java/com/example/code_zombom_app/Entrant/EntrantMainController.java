package com.example.code_zombom_app.Entrant;

import android.view.View;
import android.widget.ImageButton;

import com.example.code_zombom_app.Helpers.MVC.GController;

public class EntrantMainController extends GController<EntrantMainModel> {
    private ImageButton imageButtonFilter;
    private final ImageButton imageButtonProfile;
    private ImageButton imageButtonCamera;

    public EntrantMainController(EntrantMainModel M,
                                 ImageButton filter, ImageButton profile, ImageButton camera) {
        super(M);
        imageButtonFilter = filter;
        imageButtonProfile = profile;
        imageButtonCamera = camera;
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
