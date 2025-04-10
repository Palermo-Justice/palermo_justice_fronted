package com.badlogic.palermojustice.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.palermojustice.Main;
import com.badlogic.palermojustice.firebase.FirebaseInterface;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    private static final String TAG = "AndroidLauncher";
    private FirebaseAnalytics firebaseAnalytics;
    private NetworkController networkController;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: Starting application initialization");

        // Verify Firebase is properly initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e(TAG, "Firebase not initialized! Initializing now...");
            FirebaseApp.initializeApp(this);
        }

        // Check Firebase database availability
        if (FirebaseDatabase.getInstance() == null) {
            Log.e(TAG, "Firebase Database instance is null!");
            // Handle this error case
            return;
        }

        // Initialize the NetworkController with context and get the instance
        Log.d(TAG, "onCreate: Initializing NetworkController");
        try {
            networkController = NetworkController.initialize(this);
            Log.d(TAG, "onCreate: NetworkController initialized successfully");

            // TEST: Basic Firebase write operation with timeout
            boolean testResult = testFirebaseConnectionWithTimeout(5000); // 5 second timeout
            Log.d(TAG, "Firebase connection test result: " + (testResult ? "SUCCESS" : "FAILED"));

            if (!testResult) {
                Log.e(TAG, "Firebase connection test failed - check your Firebase configuration");
            }

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize NetworkController", e);
        }

        // Example of directly using the NetworkController to connect to a room
        String roomId = "test_room_123";
        String playerName = "Player_" + System.currentTimeMillis() % 1000;

        Log.d(TAG, "onCreate: Attempting to connect to room: " + roomId + " as player: " + playerName);

        try {
            AtomicBoolean connectionSucceeded = new AtomicBoolean(false);
            CountDownLatch connectionLatch = new CountDownLatch(1);

            networkController.connectToRoom(roomId, playerName, success -> {
                connectionSucceeded.set(success);
                Log.d(TAG, "connectToRoom callback executed with result: " + success);

                if (success) {
                    Log.d(TAG, "connectToRoom callback: Successfully connected to room: " + roomId);

                    // Example of sending a message after connection
                    try {
                        Log.d(TAG, "connectToRoom callback: Sending PLAYER_READY message");
                        Map<String, Object> data = new HashMap<>();
                        data.put("playerName", playerName);
                        data.put("timestamp", System.currentTimeMillis());
                        data.put("platform", "Android");

                        networkController.sendMessage("PLAYER_READY", data);
                        Log.d(TAG, "connectToRoom callback: PLAYER_READY message sent successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "connectToRoom callback: Error sending message", e);
                    }
                } else {
                    Log.e(TAG, "connectToRoom callback: Failed to connect to room: " + roomId);
                }

                connectionLatch.countDown();
                return null; // Required for Java -> Kotlin lambda
            });

            Log.d(TAG, "onCreate: connectToRoom method called (async operation started)");

            // Wait for up to 5 seconds for connection to complete
            boolean completed = connectionLatch.await(5, TimeUnit.SECONDS);

            if (!completed) {
                Log.e(TAG, "Connection timed out after 5 seconds");
            } else {
                Log.d(TAG, "Connection completed with result: " + connectionSucceeded.get());
            }

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Exception when calling connectToRoom", e);
        }

        // Configure and initialize the application
        Log.d(TAG, "onCreate: Configuring libGDX application");
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Recommended, but not required.

        // Pass the Firebase service to the Main class
        Log.d(TAG, "onCreate: Initializing Main application with NetworkController");
        try {
            initialize(new Main(networkController), configuration);
            Log.d(TAG, "onCreate: Main application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize Main application", e);
        }

        Log.d(TAG, "onCreate: Application startup complete");
    }

    /**
     * Test Firebase connection with timeout to ensure we get a result
     */
    private boolean testFirebaseConnectionWithTimeout(long timeoutMs) {
        Log.d(TAG, "testFirebaseConnectionWithTimeout: Starting Firebase test with " + timeoutMs + "ms timeout");

        final AtomicBoolean testResult = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            // Get a reference to the root of the database
            DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("test_connection");
            FirebaseDatabase.getInstance().goOnline(); // Ensure connection is online

            // Create a simple timestamp value
            Map<String, Object> testData = new HashMap<>();
            testData.put("timestamp", System.currentTimeMillis());
            testData.put("device", android.os.Build.MODEL);
            testData.put("test_id", "direct_write_test");

            Log.d(TAG, "testFirebaseConnectionWithTimeout: Attempting to write test data");

            // Write data and add listeners
            testRef.setValue(testData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "testFirebaseConnectionWithTimeout: Basic write test SUCCESSFUL");
                    testResult.set(true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "testFirebaseConnectionWithTimeout: Basic write test FAILED", e);
                    testResult.set(false);
                    latch.countDown();
                });

            // Wait for the result with timeout
            boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);

            if (!completed) {
                Log.e(TAG, "testFirebaseConnectionWithTimeout: Test TIMED OUT after " + timeoutMs + "ms");
                return false;
            }

            return testResult.get();

        } catch (Exception e) {
            Log.e(TAG, "testFirebaseConnectionWithTimeout: Exception during test", e);
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Application resumed");
        // Ensure Firebase is online
        FirebaseDatabase.getInstance().goOnline();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Application paused");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Application being destroyed, cleaning up resources");

        // Disconnect from Firebase when the app is closed
        if (networkController != null) {
            try {
                Log.d(TAG, "onDestroy: Disconnecting from Firebase");
                networkController.disconnect();
                Log.d(TAG, "onDestroy: Successfully disconnected from Firebase");
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: Error disconnecting from Firebase", e);
            }
        } else {
            Log.w(TAG, "onDestroy: NetworkController was null, nothing to disconnect");
        }

        super.onDestroy();
        Log.d(TAG, "onDestroy: Application destroyed");
    }
}
