package com.badlogic.palermojustice.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.palermojustice.Main;
import com.badlogic.palermojustice.firebase.FirebaseInterface;
import com.google.firebase.analytics.FirebaseAnalytics;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create Firebase implementation for Android
        // Initialize the NetworkController with context and get the instance
        FirebaseInterface firebaseService = NetworkController.initialize(this);
        //firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Log an event to Firebase
        //Bundle bundle = new Bundle();
        //bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "test_event");
        //firebaseAnalytics.logEvent("app_open", bundle);

        // Configure and initialize the application
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Recommended, but not required.

        // Pass the Firebase service to the Main class
        initialize(new Main(firebaseService), configuration);
    }
}
