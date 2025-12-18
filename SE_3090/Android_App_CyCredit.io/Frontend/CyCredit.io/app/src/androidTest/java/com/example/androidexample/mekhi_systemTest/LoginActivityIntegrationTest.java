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

import cycredit.io.LoginActivity;
import cycredit.io.R;
import cycredit.io.Session;

/**
 * Integration tests for Login Activity
 * Tests: Login form, signup navigation, password change, user deletion
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityIntegrationTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Session.setUser(context, 1, "TestUser");
    }

    @Test
    public void testActivityLaunches() {
        // Wait for activity to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Verify activity loaded
        Espresso.onView(ViewMatchers.withId(android.R.id.content))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testUsernameEditTextExists() {
        Espresso.onView(ViewMatchers.withId(R.id.login_username_edt))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testPasswordEditTextExists() {
        Espresso.onView(ViewMatchers.withId(R.id.login_password_edt))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testLoginButtonExists() {
        Espresso.onView(ViewMatchers.withId(R.id.login_login_btn))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testSignupButtonExists() {
        Espresso.onView(ViewMatchers.withId(R.id.login_signup_btn))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testRememberMeCheckboxExists() {
        Espresso.onView(ViewMatchers.withId(R.id.cb_remember_me))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testBackButtonExists() {
        Espresso.onView(ViewMatchers.withId(R.id.back_btn))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testCanTypeUsername() {
        Espresso.onView(ViewMatchers.withId(R.id.login_username_edt))
                .perform(ViewActions.typeText("testuser"));
    }

    @Test
    public void testCanTypePassword() {
        Espresso.onView(ViewMatchers.withId(R.id.login_password_edt))
                .perform(ViewActions.typeText("testpass"));
    }

    @Test
    public void testLoginButtonClickable() {
        Espresso.onView(ViewMatchers.withId(R.id.login_login_btn))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()));
    }

    @Test
    public void testSignupButtonClickable() {
        Espresso.onView(ViewMatchers.withId(R.id.login_signup_btn))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()));
    }

    @Test
    public void testRememberMeCheckboxClickable() {
        Espresso.onView(ViewMatchers.withId(R.id.cb_remember_me))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()));
    }

    @Test
    public void testBackButtonClickable() {
        Espresso.onView(ViewMatchers.withId(R.id.back_btn))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()));
    }

    @Test
    public void testDeleteUserButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.delete_user_btn))
                .check(existsAssertion);
    }

    @Test
    public void testChangePassButtonExists() {
        ViewAssertion existsAssertion = (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
        };
        Espresso.onView(ViewMatchers.withId(R.id.change_pass_btn))
                .check(existsAssertion);
    }

    @Test
    public void testLoginButtonWithEmptyFields() {
        // Clear any existing text
        Espresso.onView(ViewMatchers.withId(R.id.login_username_edt))
                .perform(ViewActions.clearText());
        Espresso.onView(ViewMatchers.withId(R.id.login_password_edt))
                .perform(ViewActions.clearText());
        
        // Click login button - should trigger validation
        Espresso.onView(ViewMatchers.withId(R.id.login_login_btn))
                .perform(ViewActions.click());
        
        // Wait a bit for dialog to appear
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSignupButtonNavigation() {
        // Click signup button - should navigate to SignupActivity
        Espresso.onView(ViewMatchers.withId(R.id.login_signup_btn))
                .perform(ViewActions.click());
        
        // Wait for navigation
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRememberMeToggle() {
        // Click remember me checkbox
        Espresso.onView(ViewMatchers.withId(R.id.cb_remember_me))
                .perform(ViewActions.click());
        
        // Verify it's checked
        Espresso.onView(ViewMatchers.withId(R.id.cb_remember_me))
                .check(ViewAssertions.matches(ViewMatchers.isChecked()));
    }

    @Test
    public void testFormInputFlow() {
        // Test complete form input flow
        Espresso.onView(ViewMatchers.withId(R.id.login_username_edt))
                .perform(ViewActions.clearText())
                .perform(ViewActions.typeText("testuser@iastate.edu"));
        
        Espresso.onView(ViewMatchers.withId(R.id.login_password_edt))
                .perform(ViewActions.clearText())
                .perform(ViewActions.typeText("password123"));
        
        // Verify text was entered
        Espresso.onView(ViewMatchers.withId(R.id.login_username_edt))
                .check(ViewAssertions.matches(ViewMatchers.withText("testuser@iastate.edu")));
    }
}

