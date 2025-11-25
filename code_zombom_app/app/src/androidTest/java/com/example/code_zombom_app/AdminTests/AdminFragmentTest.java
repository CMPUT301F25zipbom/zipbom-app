package com.example.code_zombom_app.AdminTests;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.code_zombom_app.R;
import com.example.code_zombom_app.mockDB;
import com.example.code_zombom_app.ui.admin.EventsAdminFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for EventsAdminFragment using the reusable mockDB.
 * Covers US 03.04.01: Admin can browse events.
 */
@RunWith(AndroidJUnit4.class)
public class AdminFragmentTest {

    @Test
    public void testAdminCanBrowseEventsWithMockDb() {
        // Create the fake database
        mockDB fakeDb = new mockDB();

        // Preload some events
        Map<String, Object> event1 = new HashMap<>();
        event1.put("Name", "Halloween Party");
        event1.put("Max People", "50");
        event1.put("Date", "Oct 31");
        event1.put("Deadline", "Oct 25");
        event1.put("Genre", "Horror");
        event1.put("Location", "Campus Hall");
        fakeDb.addEvent("event123", event1);

        Map<String, Object> event2 = new HashMap<>();
        event2.put("Name", "Winter Gala");
        event2.put("Max People", "100");
        event2.put("Date", "Dec 20");
        event2.put("Deadline", "Dec 10");
        event2.put("Genre", "Formal");
        event2.put("Location", "Main Auditorium");
        fakeDb.addEvent("event456", event2);

        // 3️⃣ Launch the fragment
        FragmentScenario<EventsAdminFragment> scenario =
                FragmentScenario.launchInContainer(EventsAdminFragment.class);

        scenario.onFragment(fragment -> {
            // 4️⃣ Inject the fake database
            fragment.setMockDatabase(null, fakeDb.mockEventsCollection);

            // 5️⃣ Wait for the listener to populate the container
            fragment.getView().post(() -> {
                LinearLayout container = fragment.eventsContainer;

                // 6️⃣ Verify two events are displayed
                assert(container.getChildCount() == 2);

                // 7️⃣ Verify event details
                TextView firstEvent = container.getChildAt(0).findViewById(R.id.textView_event_list_items_details);
                TextView secondEvent = container.getChildAt(1).findViewById(R.id.textView_event_list_items_details);

                String firstText = firstEvent.getText().toString();
                String secondText = secondEvent.getText().toString();

                assert(firstText.contains("Halloween Party"));
                assert(firstText.contains("Oct 31"));
                assert(firstText.contains("Campus Hall"));

                assert(secondText.contains("Winter Gala"));
                assert(secondText.contains("Dec 20"));
                assert(secondText.contains("Main Auditorium"));
            });
        });
    }
}
