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

import cycredit.io.QuizActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for Parks Library QuizActivity (Demo 4 Feature)
 * Tests: Question display, submission, rewards, progress tracking
 */
@RunWith(AndroidJUnit4.class)
public class QuizActivityIntegrationTest {

    private static final int TEST_USER_ID = 1;

    @Rule
    public ActivityScenarioRule<QuizActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    private Intent createIntent() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                QuizActivity.class);
        intent.putExtra("USER_ID", TEST_USER_ID);
        return intent;
    }

    @Before
    public void setUp() {
        // Set up test session if needed
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Session.setUser(context, TEST_USER_ID, "TestUser");
    }

    @Test
    public void testActivityLaunches() {
        // Verify activity title (check the actual displayed text in the layout)
        Espresso.onView(ViewMatchers.withText("Parks Library Challenge"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testProgressSummaryDisplayed() {
        // Verify progress summary is visible
        Espresso.onView(ViewMatchers.withId(R.id.progress_summary))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testTopicSpinnerExists() {
        // Verify topic filter spinner is present
        Espresso.onView(ViewMatchers.withId(R.id.topic_spinner))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testDifficultySpinnerExists() {
        // Verify difficulty filter spinner is present
        Espresso.onView(ViewMatchers.withId(R.id.difficulty_spinner))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testRefreshButtonExists() {
        // Verify refresh button is present
        Espresso.onView(ViewMatchers.withId(R.id.refresh_button))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testSwipeRefreshWorks() {
        // Test pull-to-refresh functionality
        Espresso.onView(ViewMatchers.withId(R.id.swipe_refresh))
                .perform(ViewActions.swipeDown());
        
        // Verify refresh indicator appears (may be brief)
        // Note: Actual API call testing would require mocking
    }

    @Test
    public void testQuestionContainerExists() {
        // Verify question container is present
        Espresso.onView(ViewMatchers.withId(R.id.question_container))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testSubmitButtonExists() {
        // Verify submit button is present (may be hidden initially)
        Espresso.onView(ViewMatchers.withId(R.id.submit_quiz_btn))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testEmptyStateHandling() {
        // Verify empty state view exists in the hierarchy (may be GONE by default)
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            // View exists in hierarchy - test passes
        };
        Espresso.onView(ViewMatchers.withId(R.id.empty_state))
                .check(existsAssertion);
    }

    @Test
    public void testProgressBarExists() {
        // Verify progress bar exists in the hierarchy (may be GONE by default)
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            // View exists in hierarchy - test passes
        };
        Espresso.onView(ViewMatchers.withId(R.id.progress_bar))
                .check(existsAssertion);
    }

    @Test
    public void testQuestionTextExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.question_text))
                .check(existsAssertion);
    }

    @Test
    public void testQuestionTopicExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.question_topic))
                .check(existsAssertion);
    }

    @Test
    public void testOptionsGroupExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.quiz_options))
                .check(existsAssertion);
    }

    @Test
    public void testNextButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.next_question_btn))
                .check(existsAssertion);
    }

    @Test
    public void testResultTextExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.result_text))
                .check(existsAssertion);
    }

    @Test
    public void testRewardTextExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.reward_text))
                .check(existsAssertion);
    }

    @Test
    public void testBackButtonExists() {
        Espresso.onView(ViewMatchers.withId(R.id.btn_back_to_map))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    /**
     * Note: Full integration tests with API mocking would require:
     * 1. Mocking Volley RequestQueue
     * 2. Mocking API responses
     * 3. Testing question submission flow
     * 4. Testing reward display
     * 5. Testing HUD refresh after rewards
     * 
     * These tests verify UI components are present and accessible.
     * For full API integration testing, see backend tests.
     */
}

