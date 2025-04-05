package com.badlogic.palermojustice

import com.badlogic.gdx.Game
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.firebase.FirebaseInterface
import com.badlogic.palermojustice.view.HomeScreen

class Main : Game {
    companion object {
        lateinit var instance: Main
            private set
    }

    // Constructor without parameters (for Desktop mock)
    constructor() {
        // Nothing
    }

    constructor(firebaseService: FirebaseInterface) {
        GameController.getInstance().setNetworkController(firebaseService)
    }

    override fun create() {
        instance = this
        setScreen(HomeScreen()) // Start GameScreen for the main screen
    }
}
