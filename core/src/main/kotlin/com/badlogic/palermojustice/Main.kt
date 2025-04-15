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

    lateinit var firebaseInterface: FirebaseInterface

    // Constructor without parameters (for Desktop mock)
    constructor() {
        // empty constructor
    }

    override fun create() {
        instance = this
        setScreen(HomeScreen()) // Start GameScreen for the main screen
    }
}
