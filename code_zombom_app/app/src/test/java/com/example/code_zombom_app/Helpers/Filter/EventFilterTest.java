package com.example.code_zombom_app.Helpers.Filter;

import com.example.code_zombom_app.Helpers.Event.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for EventFilter.passFilter() method (in-memory filtering).
 * Tests US 01.01.04: "As an entrant, I want to filter events based on my interests and availability."
 *
 * @author Test Suite
 */
@RunWith(MockitoJUnitRunner.class)
public class EventFilterTest {

    private EventFilter eventFilter;
    private static final String GENRE_SPORT = "Sport";
    private static final String GENRE_MUSIC = "Music";
    private static final String GENRE_FOOD = "Food";

    @Before
    public void setUp() {
        eventFilter = new EventFilter();
    }

    /**
     * Test filter by interests only: Only events matching the specified interests are returned.
     */
    @Test
    public void passFilter_FilterByInterestsOnly_ReturnsMatchingEvents() {
        // Arrange
        eventFilter.setFilterGenre(GENRE_SPORT);
        eventFilter.setFilterStartDate(null);
        eventFilter.setFilterEndDate(null);

        Event sportEvent = createTestEvent("event-1", "Sport Event", GENRE_SPORT);
        Event musicEvent = createTestEvent("event-2", "Music Event", GENRE_MUSIC);
        Event foodEvent = createTestEvent("event-3", "Food Event", GENRE_FOOD);

        // Act & Assert
        assertTrue("Sport event should pass filter", eventFilter.passFilter(sportEvent));
        assertFalse("Music event should not pass filter", eventFilter.passFilter(musicEvent));
        assertFalse("Food event should not pass filter", eventFilter.passFilter(foodEvent));
    }

    /**
     * Test filter by availability only: Only events that fit within the entrant's availability
     * (or do not conflict) are returned.
     */
    @Test
    public void passFilter_FilterByAvailabilityOnly_ReturnsMatchingEvents() {
        // Arrange
        eventFilter.setFilterGenre(null);
        
        // Set availability window: Jan 15 - Jan 20
        Date filterStart = new Date(100, 0, 15); // Jan 15, 2000
        Date filterEnd = new Date(100, 0, 20);   // Jan 20, 2000
        eventFilter.setFilterStartDate(filterStart);
        eventFilter.setFilterEndDate(filterEnd);

        // Event that overlaps with availability window (Jan 16 - Jan 18)
        Event overlappingEvent = createTestEvent("event-1", "Overlapping Event", null);
        overlappingEvent.setEventStartDate(new Date(100, 0, 16));
        overlappingEvent.setEventEndDate(new Date(100, 0, 18));

        // Event that starts before availability window (Jan 10 - Jan 12)
        Event beforeEvent = createTestEvent("event-2", "Before Event", null);
        beforeEvent.setEventStartDate(new Date(100, 0, 10));
        beforeEvent.setEventEndDate(new Date(100, 0, 12));

        // Event that starts after availability window (Jan 25 - Jan 27)
        Event afterEvent = createTestEvent("event-3", "After Event", null);
        afterEvent.setEventStartDate(new Date(100, 0, 25));
        afterEvent.setEventEndDate(new Date(100, 0, 27));

        // Act & Assert
        assertTrue("Overlapping event should pass filter", 
                eventFilter.passFilter(overlappingEvent));
        assertFalse("Event before availability should not pass filter", 
                eventFilter.passFilter(beforeEvent));
        assertFalse("Event after availability should not pass filter", 
                eventFilter.passFilter(afterEvent));
    }

    /**
     * Test combined interests + availability: Only events satisfying both criteria are returned.
     */
    @Test
    public void passFilter_CombinedInterestsAndAvailability_ReturnsEventsMatchingBoth() {
        // Arrange
        eventFilter.setFilterGenre(GENRE_SPORT);
        Date filterStart = new Date(100, 0, 15);
        Date filterEnd = new Date(100, 0, 20);
        eventFilter.setFilterStartDate(filterStart);
        eventFilter.setFilterEndDate(filterEnd);

        // Event matching both: Sport genre, overlaps availability (Jan 16 - Jan 18)
        Event matchingBoth = createTestEvent("event-1", "Matching Both", GENRE_SPORT);
        matchingBoth.setEventStartDate(new Date(100, 0, 16));
        matchingBoth.setEventEndDate(new Date(100, 0, 18));

        // Event matching interest only: Sport genre, but outside availability (Jan 25 - Jan 27)
        Event matchingInterestOnly = createTestEvent("event-2", "Matching Interest Only", GENRE_SPORT);
        matchingInterestOnly.setEventStartDate(new Date(100, 0, 25));
        matchingInterestOnly.setEventEndDate(new Date(100, 0, 27));

        // Event matching availability only: Music genre, overlaps availability (Jan 16 - Jan 18)
        Event matchingAvailabilityOnly = createTestEvent("event-3", "Matching Availability Only", GENRE_MUSIC);
        matchingAvailabilityOnly.setEventStartDate(new Date(100, 0, 16));
        matchingAvailabilityOnly.setEventEndDate(new Date(100, 0, 18));

        // Event matching neither: Food genre, outside availability (Jan 25 - Jan 27)
        Event matchingNeither = createTestEvent("event-4", "Matching Neither", GENRE_FOOD);
        matchingNeither.setEventStartDate(new Date(100, 0, 25));
        matchingNeither.setEventEndDate(new Date(100, 0, 27));

        // Act & Assert
        assertTrue("Event matching both criteria should pass", 
                eventFilter.passFilter(matchingBoth));
        assertFalse("Event matching only interest should not pass", 
                eventFilter.passFilter(matchingInterestOnly));
        assertFalse("Event matching only availability should not pass", 
                eventFilter.passFilter(matchingAvailabilityOnly));
        assertFalse("Event matching neither should not pass", 
                eventFilter.passFilter(matchingNeither));
    }

    /**
     * Test no matching events: Returns false for all events when none match.
     */
    @Test
    public void passFilter_NoMatchingEvents_AllEventsFail() {
        // Arrange
        eventFilter.setFilterGenre(GENRE_SPORT);
        Date filterStart = new Date(100, 0, 15);
        Date filterEnd = new Date(100, 0, 20);
        eventFilter.setFilterStartDate(filterStart);
        eventFilter.setFilterEndDate(filterEnd);

        // Events that don't match either criteria
        Event wrongGenre = createTestEvent("event-1", "Wrong Genre", GENRE_MUSIC);
        wrongGenre.setEventStartDate(new Date(100, 0, 16));
        wrongGenre.setEventEndDate(new Date(100, 0, 18));

        Event wrongDates = createTestEvent("event-2", "Wrong Dates", GENRE_SPORT);
        wrongDates.setEventStartDate(new Date(100, 0, 25));
        wrongDates.setEventEndDate(new Date(100, 0, 27));

        // Act & Assert
        assertFalse("Event with wrong genre should not pass", 
                eventFilter.passFilter(wrongGenre));
        assertFalse("Event with wrong dates should not pass", 
                eventFilter.passFilter(wrongDates));
    }

    /**
     * Helper method to create a test Event with default values.
     */
    private Event createTestEvent(String eventId, String name, String genre) {
        Event event = new Event(name);
        event.setEventId(eventId);
        event.setGenre(genre);
        event.setCapacity(10);
        event.setWaitlistLimit(20);
        return event;
    }
}
