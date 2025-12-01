package com.example.code_zombom_app.Helpers.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pure Event model tests for waitlist counts and lottery guidelines (US 01.05.04, US 01.05.05).
 */
@RunWith(MockitoJUnitRunner.class)
public class WaitlistCountAndGuidelinesTest {

    @Before
    public void disableQr() {
        Event.setQrCodeGenerationEnabled(false);
    }

    @Test
    public void getNumberOfWaiting_ReflectsJoinLeaveOperations() {
        Event event = new Event("Count Test");

        event.joinWaitingList("a@example.com");
        event.joinWaitingList("b@example.com");
        assertEquals(2, event.getNumberOfWaiting());

        event.leaveWaitingList("a@example.com");
        assertEquals(1, event.getNumberOfWaiting());
    }

    @Test
    public void lotteryGuidelines_AddAndRemoveGuideline() {
        Event event = new Event("Guideline Test");

        event.addLotteryGuideline("Must share interests");
        event.addLotteryGuideline("Local entrants preferred");
        assertTrue(event.getLotterySelectionGuidelines().contains("Must share interests"));
        assertEquals(2, event.getLotterySelectionGuidelines().size());

        event.removeLotteryGuideline("Must share interests");
        assertEquals(1, event.getLotterySelectionGuidelines().size());
    }
}


