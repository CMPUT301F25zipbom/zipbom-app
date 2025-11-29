package com.example.code_zombom_app.Entrant;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventListAdapter;
import com.example.code_zombom_app.Helpers.MVC.GController;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;

public class EntrantMainController extends GController<EntrantMainModel> {
    private ImageButton imageButtonFilter;
    private final ImageButton imageButtonProfile;
    private ImageButton imageButtonCamera;
    private ListView listViewEvents;
    private EventListAdapter eventListAdapter;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    public EntrantMainController(EntrantMainModel M,
                                 ImageButton filter, ImageButton profile, ImageButton camera,
                                 ListView events, EventListAdapter adapter,
                                 ActivityResultLauncher<ScanOptions> barcode) {
        super(M);
        imageButtonFilter = filter;
        imageButtonProfile = profile;
        imageButtonCamera = camera;
        listViewEvents = events;
        eventListAdapter = adapter;
        barcodeLauncher = barcode;
    }

    @Override
    public void bindView() {
        imageButtonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EntrantMainModel) model).open("Profile");
            }
        });

        imageButtonFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.requestFilter();
            }
        });

        imageButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanOptions options = new ScanOptions();
                options.setPrompt("Scan an event QR Code");
                options.setBeepEnabled(true);
                options.setOrientationLocked(true);
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                barcodeLauncher.launch(options);
            }
        });
    }
}
