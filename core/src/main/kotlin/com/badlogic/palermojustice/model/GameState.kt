package com.badlogic.palermojustice.model

object GameState {
    val players = mutableListOf<Player>().apply {
        addAll(
            listOf(
                Player("1", "Mario", Paesano()),
                Player("2", "Luigi", Mafioso()),
                Player("3", "Peach", Ispettore()),
                Player("4", "Toad", Sgarrista()),
                Player("5", "Yoshi", Paesano())
            )
        )
    }


    var currentRoleIndex = 0

    val roleSequence = listOf("Ispettore", "Sgarrista", "Mafioso")

    fun getPlayerByRole(role: String): Player? {
        return players.firstOrNull { it.role.name == role }
    }
}
