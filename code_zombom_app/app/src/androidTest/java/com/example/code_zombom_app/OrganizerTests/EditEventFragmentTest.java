package com.example.code_zombom_app.OrganizerTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.R;
import com.example.code_zombom_app.organizer.AddEventFragment;
import com.example.code_zombom_app.organizer.EditEventFragment;

/**
 * Unit test for the logic methods within EditEventFragment.
 * This test focuses on the validation methods that dont have to do with
 * photo stuff
 */
@RunWith(AndroidJUnit4.class)
public class EditEventFragmentTest {
    private EditEventFragment getTestFragment() {
        return new EditEventFragment();
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_EventAfterDeadline_ReturnsTrue() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // The event date is clearly after the deadline date. Should be valid.
            assertTrue("Event date is after deadline, should be valid", fragment.validdatechecker("Oct 31 2025", "Oct 20 2025"));
            assertTrue("Event month is after deadline month (same year), should be valid", fragment.validdatechecker("Nov 15 2025", "Oct 15 2025"));
            assertTrue("Event year is after deadline year, should be valid", fragment.validdatechecker("Jan 01 2026", "Dec 31 2025"));
        });
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_EventBeforeDeadline_ReturnsFalse() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // The event is before the deadline. Should be invalid.
            assertFalse("Event date is before deadline, should be invalid", fragment.validdatechecker("Oct 20 2025", "Oct 31 2025"));
            assertFalse("Event month is before deadline month, should be invalid", fragment.validdatechecker("Oct 15 2025", "Nov 15 2025"));
            assertFalse("Event year is before deadline year, should be invalid", fragment.validdatechecker("Dec 31 2025", "Jan 01 2026"));
        });
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_EventSameAsDeadline_ReturnsFalse() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // The deadline must be strictly before the event. Same day is invalid.
            assertFalse("Event and deadline on the same day should be invalid", fragment.validdatechecker("Nov 01 2025", "Nov 01 2025"));
        });
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_MalformedInput_ReturnsFalse() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // Test various incorrect formats.
            // Now the fragment has a context, so Toast.makeText() will not crash.
            assertFalse("Invalid year format should be invalid", fragment.validdatechecker("Jan 01 202a", "Jan 01 2025"));
            assertFalse("Invalid day format should be invalid", fragment.validdatechecker("Jan bb 2025", "Jan 01 2025"));
            assertFalse("Invalid month format should be invalid", fragment.validdatechecker("Jann 01 2025", "Jan 01 2025"));
            assertFalse("Incomplete date string should be invalid", fragment.validdatechecker("Jan 01", "Jan 01 2025"));
            assertFalse("Empty date string should be invalid", fragment.validdatechecker("", "Jan 01 2025"));
        });
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_CaseInsensitiveMonth_ReturnsTrue() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // The method should handle different cases for the month.
            assertTrue("Month case should not matter", fragment.validdatechecker("nOV 15 2025", "oCT 15 2025"));
        });
    }

    /**
     * Tests the maxentrantchecker method.
     */
    @Test
    public void maxentrantchecker_PositiveNumber_ReturnsTrue() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            assertTrue("A positive number string should be valid", fragment.maxentrantchecker("100"));
            assertTrue("Zero should be a valid input", fragment.maxentrantchecker("0"));
        });
    }

    /**
     * Tests the maxentrantchecker method.
     */
    @Test
    public void maxentrantchecker_EmptyString_ReturnsTrue() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // Your business logic specifies that an empty string is a valid case (means no limit).
            assertTrue("An empty string should be considered valid", fragment.maxentrantchecker(""));
        });
    }

    /**
     * Tests the maxentrantchecker method.
     */
    @Test
    public void maxentrantchecker_NegativeNumber_ReturnsFalse() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // A negative number should be invalid.
            assertFalse("A negative number string should be invalid", fragment.maxentrantchecker("-10"));
        });
    }

    /**
     * Tests the maxentrantchecker method.
     */
    @Test
    public void maxentrantchecker_NonNumericString_ReturnsFalse() {
        FragmentScenario<EditEventFragment> scenario = FragmentScenario.launchInContainer(EditEventFragment.class);
        scenario.onFragment(fragment -> {
            // A string that cannot be parsed into an integer should be invalid.
            assertFalse("A non-numeric string should be invalid", fragment.maxentrantchecker("abc"));
            assertFalse("A string with numbers and letters should be invalid", fragment.maxentrantchecker("10a"));
            assertFalse("A decimal number string should be invalid", fragment.maxentrantchecker("10.5"));
        });
    }

    /**
     * Tests The populate fields method.
     * @throws InterruptedException Has to be here because it won't run if we try to use Thread.sleep(2000);
     */
    @Test
    public void populateFields_fillsUIFromBundleAndFirestore() throws InterruptedException {

        // Create a mock "originalEventText" string, just like the one passed from OrganizerMainFragment.
        String mockEventText = "Name: event for tests\n" +
                "Max People: 1\n" +
                "Date: may 10 2001\n" +
                "Deadline: may 01 2001\n" +
                "Genre: do not delete\n" +
                "Location: " ;
        // real Id from firebase
        String mockEventId = "6dldcbbxZgR6VoegyJ20";

        // Create the arguments bundle that the fragment expects.
        Bundle args = new Bundle();
        args.putString("eventId", mockEventId);
        args.putString("eventText", mockEventText);

        // Launch the EditEventFragment WITH the prepared arguments.
        FragmentScenario.launchInContainer(EditEventFragment.class, args);

        Espresso.onView(ViewMatchers.withId(R.id.editTextName)).check(ViewAssertions.matches(ViewMatchers.withText("event for tests")));
        Espresso.onView(ViewMatchers.withId(R.id.editTextMaxPeople)).check(ViewAssertions.matches(ViewMatchers.withText("1")));
        Espresso.onView(ViewMatchers.withId(R.id.editTextDate)).check(ViewAssertions.matches(ViewMatchers.withText("may 10 2001")));
        Espresso.onView(ViewMatchers.withId(R.id.editTextDeadline)).check(ViewAssertions.matches(ViewMatchers.withText("may 01 2001")));
        Espresso.onView(ViewMatchers.withId(R.id.editTextGenre)).check(ViewAssertions.matches(ViewMatchers.withText("do not delete")));
        Espresso.onView(ViewMatchers.withId(R.id.editTextLocation)).check(ViewAssertions.matches(ViewMatchers.withText("")));

        Thread.sleep(2000); // Wait for 2 seconds to allow Firestore to respond.

        Espresso.onView(ViewMatchers.withId(R.id.editTextDescription)).check(ViewAssertions.matches(ViewMatchers.withText("Description(optional)")));
        Espresso.onView(ViewMatchers.withId(R.id.maxamountofentrants)).check(ViewAssertions.matches(ViewMatchers.withText("10")));
    }
}
