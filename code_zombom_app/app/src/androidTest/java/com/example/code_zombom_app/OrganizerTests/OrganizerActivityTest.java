package com.example.code_zombom_app.OrganizerTests;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.code_zombom_app.organizer.AddEventFragment;
import com.example.code_zombom_app.organizer.OrganizerActivity;

import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerActivityTest {
    @Rule
    ActivityScenarioRule<OrganizerActivity> scenarioRule = new ActivityScenarioRule<>(OrganizerActivity.class);


}
