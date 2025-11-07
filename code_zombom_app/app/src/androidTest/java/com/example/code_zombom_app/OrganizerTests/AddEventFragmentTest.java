package com.example.code_zombom_app.OrganizerTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.organizer.AddEventFragment;

/**
 * Unit test for the logic methods within AddEventFragment.
 * This test focuses on the validation methods `validdate` and `maxentrantchecker`.
 */
@RunWith(AndroidJUnit4.class)
public class AddEventFragmentTest {

    private FragmentScenario<AddEventFragment> scenario;
    @Before
    public void setUp() {
        // Create an instance of the fragment to test its methods.
        // Note: We are not testing UI components, only the internal logic.
        scenario = FragmentScenario.launchInContainer(AddEventFragment.class);    }

    private AddEventFragment getTestFragment() {
        return new AddEventFragment();
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_EventAfterDeadline_ReturnsTrue() {
        scenario.onFragment(fragment -> {
            // The event date is clearly after the deadline date. Should be valid.
            assertTrue("Event date is after deadline, should be valid", fragment.validdate("Oct 31 2025", "Oct 20 2025"));
            assertTrue("Event month is after deadline month (same year), should be valid", fragment.validdate("Nov 15 2025", "Oct 15 2025"));
            assertTrue("Event year is after deadline year, should be valid", fragment.validdate("Jan 01 2026", "Dec 31 2025"));
        });
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_EventBeforeDeadline_ReturnsFalse() {
        scenario.onFragment(fragment -> {
            // The event is before the deadline. Should be invalid.
            assertFalse("Event date is before deadline, should be invalid", fragment.validdate("Oct 20 2025", "Oct 31 2025"));
            assertFalse("Event month is before deadline month, should be invalid", fragment.validdate("Oct 15 2025", "Nov 15 2025"));
            assertFalse("Event year is before deadline year, should be invalid", fragment.validdate("Dec 31 2025", "Jan 01 2026"));
        });
    }

    /**
     * Tests the validdate method.
     */
    @Test
    public void validdate_EventSameAsDeadline_ReturnsFalse() {
        scenario.onFragment(fragment -> {
            // The deadline must be strictly before the event. Same day is invalid.
            assertFalse("Event and deadline on the same day should be invalid", fragment.validdate("Nov 01 2025", "Nov 01 2025"));
        });
    }

    /**
     * Tests the validdate method
     */
    @Test
    public void validdate_MalformedInput_ReturnsFalse() {
        scenario.onFragment(fragment -> {
            // Test various incorrect formats.
            // Now the fragment has a context, so Toast.makeText() will not crash.
            assertFalse("Invalid year format should be invalid", fragment.validdate("Jan 01 202a", "Jan 01 2025"));
            assertFalse("Invalid day format should be invalid", fragment.validdate("Jan bb 2025", "Jan 01 2025"));
            assertFalse("Invalid month format should be invalid", fragment.validdate("Jann 01 2025", "Jan 01 2025"));
            assertFalse("Incomplete date string should be invalid", fragment.validdate("Jan 01", "Jan 01 2025"));
            assertFalse("Empty date string should be invalid", fragment.validdate("", "Jan 01 2025"));
        });
    }

    /**
     * Tests the validdate method
     */
    @Test
    public void validdate_CaseInsensitiveMonth_ReturnsTrue() {
        scenario.onFragment(fragment -> {
            // The method should handle different cases for the month.
            assertTrue("Month case should not matter", fragment.validdate("nOV 15 2025", "oCT 15 2025"));
        });
    }

    /**
     * Tests the maxentrantchecker method.
     */
    @Test
    public void maxentrantchecker_PositiveNumber_ReturnsTrue() {
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
        scenario.onFragment(fragment -> {
            // A string that cannot be parsed into an integer should be invalid.
            assertFalse("A non-numeric string should be invalid", fragment.maxentrantchecker("abc"));
            assertFalse("A string with numbers and letters should be invalid", fragment.maxentrantchecker("10a"));
            assertFalse("A decimal number string should be invalid", fragment.maxentrantchecker("10.5"));
        });
    }
}

