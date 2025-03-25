package com.badlogic.palermojustice

import com.badlogic.gdx.Game
import com.badlogic.palermojustice.view.GameScreen
import com.badlogic.palermojustice.view.SettingsScreen

class Main : Game() {
    override fun create() {
        setScreen(SettingsScreen()) // Start GameScreen for the main screen

    }
}
//
