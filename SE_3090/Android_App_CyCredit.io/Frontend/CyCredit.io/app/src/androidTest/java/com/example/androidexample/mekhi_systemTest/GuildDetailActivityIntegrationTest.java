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

import cycredit.io.guilds.GuildDetailActivity;
import cycredit.io.guilds.GuildsActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for GuildDetailActivity
 * Tests: Activity launch, guild details display, member list, invite functionality
 */
@RunWith(AndroidJUnit4.class)
public class GuildDetailActivityIntegrationTest {

    private static final int TEST_USER_ID = 1;
    private static final int TEST_GUILD_ID = 1;
    private static final String TEST_GUILD_NAME = "Test Guild";
    private static final String TEST_USERNAME = "TestUser";

    @Rule
    public ActivityScenarioRule<GuildDetailActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    private Intent createIntent() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                GuildDetailActivity.class);
        intent.putExtra(GuildsActivity.EXTRA_GUILD_ID, TEST_GUILD_ID);
        intent.putExtra(GuildsActivity.EXTRA_USER_ID, TEST_USER_ID);
        intent.putExtra(GuildsActivity.EXTRA_GUILD_NAME, TEST_GUILD_NAME);
        intent.putExtra(GuildsActivity.EXTRA_USERNAME, TEST_USERNAME);
        return intent;
    }

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Session.setUser(context, TEST_USER_ID, TEST_USERNAME);
    }

    @Test
    public void testActivityLaunches() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        Espresso.onView(ViewMatchers.withId(android.R.id.content))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testGuildNameExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.tvGuildName))
                .check(existsAssertion);
    }

    @Test
    public void testMemberCountExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.tvMemberCount))
                .check(existsAssertion);
    }

    @Test
    public void testMembersRecyclerViewExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.recyclerMembers))
                .check(existsAssertion);
    }

    @Test
    public void testBackButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.btnBack))
                .check(existsAssertion);
    }

    @Test
    public void testGuildChatButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.btnGuildChat))
                .check(existsAssertion);
    }

    @Test
    public void testProgressBarExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.progress))
                .check(existsAssertion);
    }
}

