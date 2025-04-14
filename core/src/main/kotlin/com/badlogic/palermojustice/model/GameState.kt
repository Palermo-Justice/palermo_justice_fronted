package com.badlogic.palermojustice.model

import com.mygame.model.com.badlogic.palermojustice.Player


object GameState {
    val players = mutableListOf<Player>()
    val roleSequence = listOf("Ispettore", "Mafioso", "Sgarrista")
    var currentRoleIndex = 0
    var currentRolePlayers: MutableList<Player> = mutableListOf()
    var currentActingPlayerIndex = 0

    fun assignRoles() {
        val roles = listOf(
            Mafioso(),
            Ispettore(),
            Sgarrista(),
            Paesano()
            // Add or remove based on player count
        ).shuffled()

        for ((player, role) in players.zip(roles)) {
            player.role = role
        }
    }
}
