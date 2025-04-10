package com.badlogic.palermojustice.model

import com.mygame.model.com.badlogic.palermojustice.Player


object GameState {
    val players = mutableListOf<Player>()
    var currentRoleIndex = 0

    val roleSequence = listOf("Ispettore", "Sgarrista", "Mafioso")

    fun getPlayerByRole(role: String): Player? {
        return players.firstOrNull { it.role.name == role }
    }
}
