package com.example.code_zombom_app.Organizer;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Location.Location;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Organizer-focused tests for event registration period, capacity, waitlist limit,
 * and geolocation (US 02.01.04, 02.02.03, 02.03.01).
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizerEventSettingsTest {

    @Before
    public void setUp() {
        Event.setQrCodeGenerationEnabled(false);
    }

    @Test
    public void setRegistrationPeriod_StartAndEndDatesPersisted() {
        Event event = new Event("Period Test");

        Date start = new Date(System.currentTimeMillis());
        Date end = new Date(System.currentTimeMillis() + 86400000L);

        event.setEventStartDate(start);
        event.setEventEndDate(end);

        assertEquals(start, event.getEventStartDate());
        assertEquals(end, event.getEventEndDate());
    }

    @Test
    public void setCapacityAndWaitlistLimit_NegativeValuesClampedToZero() {
        Event event = new Event("Settings Test");

        event.setCapacity(-10);
        event.setWaitlistLimit(-5);

        assertEquals(0, event.getCapacity());
        assertEquals(0, event.getWaitlistLimit());
    }

    @Test
    public void setCapacityAndWaitlistLimit_PositiveValuesStored() {
        Event event = new Event("Settings Test");

        event.setCapacity(50);
        event.setWaitlistLimit(100);

        assertEquals(50, event.getCapacity());
        assertEquals(100, event.getWaitlistLimit());
    }

    @Test
    public void setLocation_GeolocationRequirement_StoresLocationObject() {
        Event event = new Event("Location Test");

        Location loc = new Location();
        loc.setName("Map Venue");

        event.setLocation(loc);

        assertNotNull(event.getLocation());
        assertEquals("Map Venue", event.getLocation().getName());
    }

    @Test
    public void posterUrl_UploadAndUpdate_PersistsUrl() {
        Event event = new Event("Poster Test");

        event.setPosterUrl(" https://example.com/poster1.jpg ");
        assertEquals("https://example.com/poster1.jpg", event.getPosterUrl());

        event.setPosterUrl("https://example.com/poster2.png");
        assertEquals("https://example.com/poster2.png", event.getPosterUrl());

        event.setPosterUrl(null);
        assertEquals("", event.getPosterUrl());
    }
}
