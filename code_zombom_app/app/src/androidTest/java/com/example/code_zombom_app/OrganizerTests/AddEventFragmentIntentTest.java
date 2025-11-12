package com.example.code_zombom_app.OrganizerTests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.OrganizerActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.core.AllOf.allOf;

import java.util.concurrent.CompletableFuture;

/**
 * Intent test for AddEventFragment.
 * This test verifies that UI interactions launch the correct intents.
 */
@RunWith(AndroidJUnit4.class)
public class AddEventFragmentIntentTest {

    /**
     * JUnit rule that initializes Espresso-Intents before each test
     *  and releases it after. It also launches the specified activity.
     */
    @Rule
    public IntentsTestRule<OrganizerActivity> intentsTestRule = new IntentsTestRule<>(OrganizerActivity.class);

    /**
     * Sets up the AddEeventFragment
     * @throws Exception gets thrown when something goes wrong
     */
    @Before
    public void setUp() throws Exception {
        // Navigate to the AddEventFragment before each test.
        onView(withId(R.id.toOrganizerUIForNow)).perform(click());
        onData(anything())
                .inAdapterView(withId(R.id.events_container_linearlayout))
                .atPosition(0) // Target the first item
                .perform(click());
        onView(withId(R.id.add_event_button)).perform(click());
    }

    /**
     * This tests our add photo button. Makes sure it works as intended
     */
    @Test
    public void clickAddPhotoButton_launchesImagePickerIntent() {
        // We assume the setup has already navigated us to the AddEventFragment.

        // Create a fake result for the intent. We don't want to actually open the gallery.
        // Espresso-Intents will catch the intent and respond with this fake result instead.
        Intent resultData = new Intent();
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

        // Stub the intent. Tell Espresso that any intent matching ACTION_PICK should be blocked
        // and should return our fake 'result'.
        intending(hasAction(Intent.ACTION_PICK)).respondWith(result);

        // Perform the action: click the "Upload Photo" button.
        onView(withId(R.id.buttonUploadPhoto)).perform(click());

        // Verify that the correct intent was sent.
        // We check that an intent was launched that has the action ACTION_PICK AND the type "image/*".
        intended(allOf(hasAction(Intent.ACTION_PICK), hasType("image/*")));    }

}
