package com.example.androidexample;

import android.content.Context;
import android.content.Intent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import cycredit.io.HomeActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for Home Activity
 * Tests: WebView display, navigation, audio controls
 */
@RunWith(AndroidJUnit4.class)
public class HomeActivityIntegrationTest {

    @Rule
    public ActivityScenarioRule<HomeActivity> activityRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Session.setUser(context, 1, "TestUser");
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
    public void testWebViewExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.homeWebView))
                .check(existsAssertion);
    }
}

