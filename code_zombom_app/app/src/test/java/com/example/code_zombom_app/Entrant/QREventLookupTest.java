package com.example.code_zombom_app.Entrant;

import com.example.code_zombom_app.Helpers.Event.Event;
import com.example.code_zombom_app.Helpers.Event.EventMapper;
import com.example.code_zombom_app.Helpers.Location.Location;
import com.example.code_zombom_app.organizer.EventForOrg;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests QR payload generation and domain mapping used after scanning event codes (US 01.06.01, US 01.06.02).
 */
@RunWith(MockitoJUnitRunner.class)
public class QREventLookupTest {

    @Before
    public void disableQr() {
        Event.setQrCodeGenerationEnabled(false);
    }

    @Test
    public void buildQrPayload_IncludesEventDetailsAndPoster() {
        Event event = new Event("QR View Event");
        Location location = new Location();
        location.setName("Library Hall");
        event.setLocation(location);
        event.setDescription("Study session");
        event.setEventStartDate(new java.util.Date(0));
        event.setEventEndDate(new java.util.Date(3600000));

        String payload = EventMapper.buildQrPayload(event, "https://poster.example.com/p.png");

        assertTrue(payload.contains("QR View Event"));
        assertTrue(payload.contains("Library Hall"));
        assertTrue(payload.contains("Study session"));
        assertTrue(payload.contains("poster.example.com"));
    }

    @Test
    public void toDomain_PopulatesWaitingChosenAndPendingLists() {
        EventForOrg dto = new EventForOrg();
        dto.setName("Mapped Event");
        dto.setMax_People("10");
        dto.setWait_List_Maximum("5");
        java.util.ArrayList<String> waiting = new java.util.ArrayList<>();
        waiting.add("waiting@example.com");
        dto.setEntrants(waiting);
        java.util.ArrayList<String> chosen = new java.util.ArrayList<>();
        chosen.add("chosen@example.com");
        dto.setLottery_Winners(chosen);
        java.util.ArrayList<String> pending = new java.util.ArrayList<>();
        pending.add("pending@example.com");
        dto.setAccepted_Entrants(pending);

        Event domain = EventMapper.toDomain(dto, "firestoreId123");

        assertNotNull(domain);
        assertEquals(1, domain.getWaitingList().size());
        assertEquals(1, domain.getChosenList().size());
        assertEquals(1, domain.getPendingList().size());
    }
}


