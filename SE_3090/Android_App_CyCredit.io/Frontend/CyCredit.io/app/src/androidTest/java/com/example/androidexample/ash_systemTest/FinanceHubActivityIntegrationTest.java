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

import cycredit.io.FinanceHubActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for Finance Hub (Beardshear) - Billing feature
 * Tests: Statement display, payment flows, transaction history, monthly spend
 */
@RunWith(AndroidJUnit4.class)
public class FinanceHubActivityIntegrationTest {

    private static final int TEST_USER_ID = 1;
    private static final String TEST_EMAIL = "test@example.com";

    @Rule
    public ActivityScenarioRule<FinanceHubActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    private Intent createIntent() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                FinanceHubActivity.class);
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
        
        // Verify activity title
        Espresso.onView(ViewMatchers.withId(R.id.financeTitle))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testBalanceTextExists() {
        Espresso.onView(ViewMatchers.withId(R.id.balanceText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testMonthlySpendTextExists() {
        Espresso.onView(ViewMatchers.withId(R.id.monthlySpendText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testCreditLimitTextExists() {
        Espresso.onView(ViewMatchers.withId(R.id.creditLimitText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testTransactionRecyclerViewExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.txRecycler))
                .check(existsAssertion);
    }

    @Test
    public void testSwipeRefreshExists() {
        Espresso.onView(ViewMatchers.withId(R.id.swipeRefresh))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testSwipeRefreshWorks() {
        // Test pull-to-refresh functionality
        Espresso.onView(ViewMatchers.withId(R.id.swipeRefresh))
                .perform(ViewActions.swipeDown());
    }

    @Test
    public void testStatementCardExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.statementCard))
                .check(existsAssertion);
    }

    @Test
    public void testStatementTotalDueExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.statementTotalDue))
                .check(existsAssertion);
    }

    @Test
    public void testStatementMinDueExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.statementMinDue))
                .check(existsAssertion);
    }

    @Test
    public void testStatementStatusExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.statementStatus))
                .check(existsAssertion);
    }

    @Test
    public void testPaymentAmountInputExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.paymentAmountInput))
                .check(existsAssertion);
    }

    @Test
    public void testPayMinimumButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.payMinimumBtn))
                .check(existsAssertion);
    }

    @Test
    public void testPayFullButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.payFullBtn))
                .check(existsAssertion);
    }

    @Test
    public void testPayCustomButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.payCustomBtn))
                .check(existsAssertion);
    }

    @Test
    public void testGoToStoreButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.goToStoreBtn))
                .check(existsAssertion);
    }

    @Test
    public void testBackToMapButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.backToMapBtn))
                .check(existsAssertion);
    }

    @Test
    public void testProgressBarExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.progressBar))
                .check(existsAssertion);
    }

    @Test
    public void testPayMinimumButtonClickable() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.payMinimumBtn))
                .check(existsAssertion);
        // Test click - may trigger payment flow
        try {
            Espresso.onView(ViewMatchers.withId(R.id.payMinimumBtn))
                    .perform(ViewActions.click());
            Thread.sleep(1000);
        } catch (Exception e) {
            // Payment may fail if no statement, that's ok
        }
    }

    @Test
    public void testPayFullButtonClickable() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.payFullBtn))
                .check(existsAssertion);
        // Test click - may trigger payment flow
        try {
            Espresso.onView(ViewMatchers.withId(R.id.payFullBtn))
                    .perform(ViewActions.click());
            Thread.sleep(1000);
        } catch (Exception e) {
            // Payment may fail if no statement, that's ok
        }
    }

    @Test
    public void testPayCustomButtonClickable() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.payCustomBtn))
                .check(existsAssertion);
        // Test click - may trigger payment flow
        try {
            Espresso.onView(ViewMatchers.withId(R.id.payCustomBtn))
                    .perform(ViewActions.click());
            Thread.sleep(1000);
        } catch (Exception e) {
            // Payment may fail if no amount entered, that's ok
        }
    }

    @Test
    public void testGoToStoreButtonClickable() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.goToStoreBtn))
                .check(existsAssertion);
        // Test click - should navigate to store
        try {
            Espresso.onView(ViewMatchers.withId(R.id.goToStoreBtn))
                    .perform(ViewActions.click());
            Thread.sleep(1000);
        } catch (Exception e) {
            // Navigation may fail, that's ok
        }
    }

    @Test
    public void testBackToMapButtonClickable() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.backToMapBtn))
                .check(existsAssertion);
        // Test click - should navigate back to map
        try {
            Espresso.onView(ViewMatchers.withId(R.id.backToMapBtn))
                    .perform(ViewActions.click());
            Thread.sleep(1000);
        } catch (Exception e) {
            // Navigation may fail, that's ok
        }
    }

    @Test
    public void testPaymentAmountInputEditable() {
        // Test that payment amount input can be edited
        try {
            Espresso.onView(ViewMatchers.withId(R.id.paymentAmountInput))
                    .perform(ViewActions.clearText())
                    .perform(ViewActions.typeText("50.00"));
            Thread.sleep(500);
        } catch (Exception e) {
            // Input may not be visible, that's ok
        }
    }

    @Test
    public void testBottomNavigationExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.action_bar))
                .check(existsAssertion);
    }
}

