package com.badlogic.palermojustice.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.palermojustice.Main;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;

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
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize NetworkController", e);
        }

        // Configure and initialize the application
        Log.d(TAG, "onCreate: Configuring libGDX application");
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Recommended, but not required.

        // Create a new Main instance and set the Firebase interface
        Main mainGame = new Main();
        // Set the firebaseInterface property directly
        mainGame.setFirebaseInterface(networkController);

        Log.d(TAG, "onCreate: Initializing Main application with NetworkController");
        try {
            initialize(mainGame, configuration);
            Log.d(TAG, "onCreate: Main application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize Main application", e);
        }

        Log.d(TAG, "onCreate: Application startup complete");
        networkController.setPlayerProtected("NOCSYC", "-OO26lULTH8PWsTtTcbx", false, aBoolean -> {
            return null;
        });


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
