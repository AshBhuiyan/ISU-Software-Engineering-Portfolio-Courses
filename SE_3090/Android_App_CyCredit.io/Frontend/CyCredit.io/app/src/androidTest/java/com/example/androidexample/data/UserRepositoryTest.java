package com.example.androidexample.data;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import cycredit.io.data.UserRepository;
import cycredit.io.util.UserPrefs;

import static org.junit.Assert.*;

/**
 * Integration tests for UserRepository
 * Note: These tests require a running backend or mocked Volley responses
 */
public class UserRepositoryTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Clear user prefs
        UserPrefs.saveUserId(context, -1);
    }

    @Test
    public void testRefreshUserFromServerWithInvalidUserId() {
        // Test that callback is called even with invalid userId
        final boolean[] callbackCalled = {false};
        UserRepository.refreshUserFromServer(context, 0, () -> {
            callbackCalled[0] = true;
        });
        
        // Wait a bit for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Callback should be called immediately for invalid userId
        assertTrue(callbackCalled[0]);
    }

    @Test
    public void testRefreshUserFromServerWithNegativeUserId() {
        final boolean[] callbackCalled = {false};
        UserRepository.refreshUserFromServer(context, -1, () -> {
            callbackCalled[0] = true;
        });
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        assertTrue(callbackCalled[0]);
    }

    @Test
    public void testRefreshUserFromServerWithNullCallback() {
        // Should not throw exception
        UserRepository.refreshUserFromServer(context, 1, null);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Test passes if no exception thrown
        assertTrue(true);
    }
}

