package com.example.code_zombom_app.OrganizerTests;// Import Espresso classes
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard; // Important for stability
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.AddEventFragment;
// You need to add this ToastMatcher class to your test directory
import com.example.code_zombom_app.ToastMatcher;


/**
 * UI and integration tests for AddEventFragment.
 * This test focuses on behavior specific to adding a new event.
 * Shared logic from BaseEventFragment is tested in BaseEventForOrgFragmentTest.
 */
@RunWith(AndroidJUnit4.class)
public class AddEventFragmentTest {

    private FragmentScenario<AddEventFragment> scenario;

    @Before
    public void setUp() {
        // Launch AddEventFragment within a container.
        // This simulates the fragment being added to an activity.
        scenario = FragmentScenario.launchInContainer(AddEventFragment.class);
    }

    // ERROR 1: getTestFragment() was unused and incorrect. It is removed.

    // ERROR 2: All `validdate` and `maxentrantchecker` tests are removed.
    // They were trying to call methods that no longer exist in AddEventFragment.
    // These validation logic tests belong in a separate test file for the base fragment.

    /**
     * Tests that validation fails if the event date is before the deadline.
     * This tests the behavior of the inherited validateInput() method from a user's perspective.
     */
    @Test
    public void testSave_EventDateBeforeDeadline_ShowsErrorToast() {
        // 1. Enter valid data for most fields
        onView(withId(R.id.editTextName)).perform(typeText("Time Travel Meeting"), closeSoftKeyboard());
        onView(withId(R.id.editTextMaxPeople)).perform(typeText("100"), closeSoftKeyboard());
        onView(withId(R.id.editTextGenre)).perform(typeText("Meeting"), closeSoftKeyboard());

        // 2. Enter an INVALID date combination
        onView(withId(R.id.editTextDate)).perform(typeText("Oct 20 2025"), closeSoftKeyboard());
        onView(withId(R.id.editTextDeadline)).perform(typeText("Oct 31 2025"), closeSoftKeyboard());

        // 3. Click the save button
        onView(withId(R.id.saveEventButton)).perform(click());

        // 4. Verify that the correct error Toast is shown.
        onView(withText("Invalid Days."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that validation fails if the event name is empty.
     */
    @Test
    public void testSave_EmptyEventName_ShowsErrorToast() {
        // 1. Ensure the event name is empty
        onView(withId(R.id.editTextName)).perform(clearText());

        // 2. Fill in other fields to avoid other errors
        onView(withId(R.id.editTextDate)).perform(typeText("Nov 15 2025"), closeSoftKeyboard());
        onView(withId(R.id.editTextDeadline)).perform(typeText("Nov 01 2025"), closeSoftKeyboard());
        onView(withId(R.id.editTextMaxPeople)).perform(typeText("100"), closeSoftKeyboard());
        onView(withId(R.id.editTextGenre)).perform(typeText("Meeting"), closeSoftKeyboard());

        // 3. Click the save button
        onView(withId(R.id.saveEventButton)).perform(click());

        // 4. Verify that the "name cannot be empty" error is shown
        onView(withText("Please fill all required fields."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that validation fails for a negative number in the entrant limit.
     */
    @Test
    public void testSave_NegativeEntrantLimit_ShowsErrorToast() {
        // 1. Fill in valid data for other fields
        onView(withId(R.id.editTextName)).perform(typeText("Valid Event"), closeSoftKeyboard());
        onView(withId(R.id.editTextDate)).perform(typeText("Nov 15 2025"), closeSoftKeyboard());
        onView(withId(R.id.editTextDeadline)).perform(typeText("Nov 01 2025"), closeSoftKeyboard());
        onView(withId(R.id.editTextGenre)).perform(typeText("Meeting"), closeSoftKeyboard());


        // 2. Enter a negative number for the limit
        onView(withId(R.id.editTextMaxPeople)).perform(typeText("-5"), closeSoftKeyboard());

        // 3. Click the save button
        onView(withId(R.id.saveEventButton)).perform(click());

        // 4. Verify the correct error Toast is shown
        onView(withText("Max entrants must be a positive number."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
