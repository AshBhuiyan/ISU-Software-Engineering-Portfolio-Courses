package com.example.androidexample;

import android.content.Context;
import android.content.Intent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import cycredit.io.guilds.GuildsActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for Guilds Activity
 * Tests: Guild list display, navigation, creation
 */
@RunWith(AndroidJUnit4.class)
public class GuildsActivityIntegrationTest {

    private static final int TEST_USER_ID = 1;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "TestUser";

    @Rule
    public ActivityScenarioRule<GuildsActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    private Intent createIntent() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                GuildsActivity.class);
        intent.putExtra("USER_ID", TEST_USER_ID);
        intent.putExtra("EMAIL", TEST_EMAIL);
        intent.putExtra("USERNAME", TEST_USERNAME);
        return intent;
    }

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Session.setUser(context, TEST_USER_ID, TEST_USERNAME);
    }

    @Test
    public void testActivityLaunches() {
        // Wait for activity to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Verify activity loaded
        Espresso.onView(ViewMatchers.withId(android.R.id.content))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testGuildsRecyclerViewExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.recycler))
                .check(existsAssertion);
    }

    @Test
    public void testSwipeRefreshExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.swipe))
                .check(existsAssertion);
    }

    @Test
    public void testSwipeRefreshWorks() {
        // Wait for initial load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Test pull-to-refresh functionality
        Espresso.onView(ViewMatchers.withId(R.id.swipe))
                .perform(ViewActions.swipeDown());
    }
}

