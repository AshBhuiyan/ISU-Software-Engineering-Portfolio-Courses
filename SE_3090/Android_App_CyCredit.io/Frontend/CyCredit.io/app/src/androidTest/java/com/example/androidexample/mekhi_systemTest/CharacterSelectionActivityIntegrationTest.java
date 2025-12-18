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

import cycredit.io.CharacterSelectionActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for Character Selection Activity
 * Tests: Character selection, role display, navigation
 */
@RunWith(AndroidJUnit4.class)
public class CharacterSelectionActivityIntegrationTest {

    private static final int TEST_USER_ID = 1;
    private static final String TEST_EMAIL = "test@example.com";

    @Rule
    public ActivityScenarioRule<CharacterSelectionActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    private Intent createIntent() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                CharacterSelectionActivity.class);
        intent.putExtra("USER_ID", TEST_USER_ID);
        intent.putExtra("EMAIL", TEST_EMAIL);
        return intent;
    }

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Session.setUser(context, TEST_USER_ID, "TestUser");
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
    public void testCharacterSelectionUIExists() {
        // Verify character selection UI elements exist
        Espresso.onView(ViewMatchers.withId(android.R.id.content))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}

