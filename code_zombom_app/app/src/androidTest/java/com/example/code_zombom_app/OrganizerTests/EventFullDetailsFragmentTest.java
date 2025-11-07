package com.example.code_zombom_app.OrganizerTests;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.EventFullDetailsFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI and Logic Test for EventFullDetailsFragment.
 * This test verifies that the fragment correctly displays all event details passed
 * to it via a bundle and subsequent Firestore calls.
 *
 * NOTE: This test requires a running emulator or device with network access to Firebase.
 */
@RunWith(AndroidJUnit4.class)
public class EventFullDetailsFragmentTest {

    @Test
    public void populateFields_fillsUIFromBundleAndFirestore() throws InterruptedException {
        // --- 1. ARRANGE ---

        // Create a mock "eventText" string, simulating what is passed from the previous screen.
        String mockEventText = "Name: Zombie Survival 101\n" +
                "Max People: 50\n" +
                "Date: Oct 31 2025\n" +
                "Deadline: Oct 15 2025\n" +
                "Genre: Survival\n" +
                "Location: The Old Warehouse";

        // IMPORTANT: Use a REAL Event ID from your Firestore database.
        // This event must have a "Description", "image_url", etc. for the test to pass.
        // For example, in Firestore:
        // Events -> your_real_event_id_for_testing -> { Description: "A detailed course...", image_url: "http://..." }
        String mockEventId = "6dldcbbxZgR6VoegyJ20"; // Using the same test event ID as in EditEventFragmentTest

        // Create the arguments bundle that the fragment expects.
        Bundle args = new Bundle();
        args.putString("eventId", mockEventId);
        args.putString("eventText", mockEventText);

        // --- ACT ---

        // Launch the EventFullDetailsFragment with the prepared arguments.
        FragmentScenario.launchInContainer(EventFullDetailsFragment.class, args);

        // --- ASSERT ---

        // Verify the views populated from the 'eventText' bundle argument.
        // Espresso will automatically wait for the UI thread to be idle.
        onView(ViewMatchers.withId(R.id.name_label)).check(matches(withText("Zombie Survival 101")));
        onView(ViewMatchers.withId(R.id.date_label)).check(matches(withText("Oct 31 2025")));
        onView(ViewMatchers.withId(R.id.genre_label)).check(matches(withText("Survival")));
        onView(ViewMatchers.withId(R.id.location_label)).check(matches(withText("The Old Warehouse")));
        onView(ViewMatchers.withId(R.id.deadline_label)).check(matches(withText("Deadline to sign up is: Oct 15 2025")));
        onView(ViewMatchers.withId(R.id.max_people_label)).check(matches(withText("Maximum attendees: 50")));

        // Verify views populated from the asynchronous Firestore call.
        // A short sleep is needed to allow the background network call to complete.
        // In a production test suite, you would use Espresso Idling Resources for this.
        Thread.sleep(2000); // Wait for 2 seconds to allow Firestore to respond.

        // Now, check the fields that were loaded from Firestore.
        // Replace with the actual values from your "6dldcbbxZgR6VoegyJ20" test document.
        onView(ViewMatchers.withId(R.id.deadline_label)).check(matches(withText("Description(optional)")));

        // We also check that the image view is displayed.
        // This confirms that Glide was likely called with a valid URL.
        onView(ViewMatchers.withId(R.id.poster_image_view)).check(matches(isDisplayed()));
    }
}
