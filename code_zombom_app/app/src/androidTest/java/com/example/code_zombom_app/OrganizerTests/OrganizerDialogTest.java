package com.example.code_zombom_app.OrganizerTests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;


import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.OrganizerActivity;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;import org.junit.Rule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.CoreMatchers.allOf;


/**
 * Because of the nature of this class this test is an
 * intent test for the OrganizerDialog that appears after event creation.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDialogTest {
    @Rule
    public IntentsTestRule<OrganizerActivity> intentsTestRule = new IntentsTestRule<>(OrganizerActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }
    @After
    public void tearDown() {
        Intents.release();
    }
    /**
     * Helper method to navigate to the dialog. This makes tests cleaner.
     */
    private void navigateToDialog() throws InterruptedException {
        // Navigate to the Organizer UI
        onView(withId(R.id.toOrganizerUIForNow)).perform(click());

        // Wait for Firestore to load the list data.
        // In a production test suite, use Idling Resources. For this, sleep is reliable.
        Thread.sleep(2500); // Wait 2.5 seconds

        // Click on the first item in the list
        onView(first(withParent(withId(R.id.events_container_linearlayout))))
                .perform(click());

        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));
    }
    // Helper matcher to get the first view from multiple results
    public static <T> Matcher<T> first(final Matcher<T> matcher) {
        return new BaseMatcher<T>() {
            boolean isFirst = true;

            @Override
            public boolean matches(final Object item) {
                if (isFirst && matcher.matches(item)) {
                    isFirst = false;
                    return true;
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("should return first matching item");
            }
        };
    }
    /**
     * Tests if clicking the "Edit" button launches the Android share intent.
     */
    @Test
    public void clickEditEventButton_launchesShareIntent() throws InterruptedException {
        navigateToDialog();
        // Ensure the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Click the "Edit Event" button
        onView(withId(R.id.button_edit_event)).perform(click());

        // Verify that we have navigated to the Edit screen by checking for a view that exists on it
        onView(withId(R.id.editTextName)).check(matches(isDisplayed()));
    }

    /**
     * This function tests to make sure the QR code button does its job correctly.
     * @throws InterruptedException Will happen if we incorrectly navigate to dialogue or if we don't end up navigating there.
     */
    @Test
    public void clickSeeDetailsButtonlaunchesShareIntent() throws InterruptedException {
        navigateToDialog();
        // Ensure the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Click the "See Details" button
        onView(withId(R.id.seeDetailsButton)).perform(click());

        // Verify that we have navigated to the Full Details screen by checking for a view on it
        onView(withId(R.id.name_label)).check(matches(isDisplayed()));
    }
    /**
     * Tests if clicking the "Share QR Code" button launches the Android share intent.
     */
    @Test
    public void clickShareQrCodeButton_launchesShareIntent() throws InterruptedException {
        navigateToDialog();
        // Ensure the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Stub the intent to prevent the actual share sheet from opening
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Click the "Generate QR" button
        onView(withId(R.id.genQRButton)).perform(click());

        // Verify that a chooser intent was launched
        intended(hasAction(Intent.ACTION_CHOOSER));
    }

    /**
     * Tests if clicking the "Cancel" button dismisses the dialog.
     */
    @Test
    public void clickCancelButton_dismissesDialog() throws InterruptedException {
        navigateToDialog();
        // Assuming the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Click the "Cancel" button
        onView(withId(R.id.cancelButton)).perform(click());

        // Verify the dialog is gone by checking that a view from the previous screen is visible again
        onView(withId(R.id.button_organizer_main_fragment_add_event)).check(matches(isDisplayed()));
    }
}
