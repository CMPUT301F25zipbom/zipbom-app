package com.example.code_zombom_app.OrganizerTests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.EditEventFragment;
import com.example.code_zombom_app.organizer.OrganizerActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

/**
 * Because of the nature of this class this test is an
 * intent test for the OrganizerDialog that appears after event creation.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDialogTest {
    @Rule
    public IntentsTestRule<OrganizerActivity> intentsTestRule = new IntentsTestRule<>(OrganizerActivity.class);

    /**
     * Helper method to navigate to the dialog. This makes tests cleaner.
     */
    private void navigateToDialog() throws InterruptedException {
        // Navigate to the Organizer UI
        onView(withId(R.id.toOrganizerUIForNow)).perform(click());

        // Wait for Firestore to load the list data.
        // In a production test suite, use Idling Resources. For this, sleep is reliable.
        Thread.sleep(2000); // Wait 2 seconds

        // Click on the first item in the list
        onData(anything()).inAdapterView(withId(R.id.events_container_linearlayout))
                .atPosition(0)
                .perform(click());
    }
    /**
     * Tests if clicking the "Edit" button launches the Android share intent.
     */
    @Test
    public void clickEditEventButton_launchesShareIntent() throws InterruptedException {
        navigateToDialog();
        // Ensure the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Stub the intent. We don't want to actually open the share sheet.
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Click the "Share QR Code" button. You may need to replace the ID.
        onView(ViewMatchers.withId(R.id.button_edit_event)).perform(click());

        // Verify that a chooser intent (the share sheet) was launched.
        intended(hasAction(Intent.ACTION_CHOOSER));
    }
    @Test
    public void clickSeeDetailsButtonlaunchesShareIntent() throws InterruptedException {
        navigateToDialog();
        // Ensure the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Stub the intent. We don't want to actually open the share sheet.
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Click the "Share QR Code" button. You may need to replace the ID.
        onView(ViewMatchers.withId(R.id.seeDetailsButton)).perform(click());

        // Verify that a chooser intent (the share sheet) was launched.
        intended(hasAction(Intent.ACTION_CHOOSER));
    }
    /**
     * Tests if clicking the "Share QR Code" button launches the Android share intent.
     */
    @Test
    public void clickShareQrCodeButton_launchesShareIntent() throws InterruptedException {
        navigateToDialog();
        // Ensure the dialog is now visible.
        onView(withId(R.id.button_start_draw)).check(matches(isDisplayed()));

        // Stub the intent. We don't want to actually open the share sheet.
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Click the "Share QR Code" button. You may need to replace the ID.
        onView(ViewMatchers.withId(R.id.genQRButton)).perform(click());

        // Verify that a chooser intent (the share sheet) was launched.
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

        // Click the "Cancel" button.
        onView(ViewMatchers.withId(R.id.cancelButton)).perform(click());

        // Verify the dialog is no longer displayed.
        onView(withId(R.id.add_event_button)).check(matches(isDisplayed()));
    }
}
