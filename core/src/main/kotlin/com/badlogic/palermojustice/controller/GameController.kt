package com.badlogic.palermojustice.controller

import com.badlogic.palermojustice.model.GameState

object GameController {
    fun startNightPhase() {
        GameState.currentRoleIndex = 0
    }
}
