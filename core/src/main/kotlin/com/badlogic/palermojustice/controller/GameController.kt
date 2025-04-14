package com.badlogic.palermojustice.controller

import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.view.GameScreen
import com.badlogic.palermojustice.view.RoleActionScreen

object GameController {
    fun startNightPhase() {
        GameState.currentRoleIndex = 0
    }

    fun showRoleScreensForAllPlayers() {

        val localPlayer = GameState.players[0] // or however you decide who this client is

        Main.instance.setScreen(GameScreen(localPlayer))
    }

}
