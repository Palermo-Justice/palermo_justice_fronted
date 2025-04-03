package com.badlogic.palermojustice

import com.badlogic.gdx.Game
import com.badlogic.palermojustice.view.HomeScreen

class Main : Game() {
    companion object {
        lateinit var instance: Main
            private set
    }

    override fun create() {
        instance = this
        setScreen(HomeScreen()) // Start GameScreen for the main screen
    }
}
