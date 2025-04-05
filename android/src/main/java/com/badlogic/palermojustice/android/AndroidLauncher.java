package com.badlogic.palermojustice.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.palermojustice.Main;
import com.badlogic.palermojustice.firebase.AndroidFirebaseService;
import com.badlogic.palermojustice.firebase.FirebaseInterface;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create Firebase implementation for Android
        FirebaseInterface firebaseService = new AndroidFirebaseService();

        // Configure and initialize the application
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Recommended, but not required.

        // Pass the Firebase service to the Main class
        initialize(new Main(firebaseService), configuration);
    }
}
