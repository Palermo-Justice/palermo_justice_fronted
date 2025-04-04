package com.badlogic.palermojustice.model

import java.util.ArrayList

enum class GamePhase {
    LOBBY,
    DAY_DISCUSSION,
    DAY_VOTING,
    NIGHT_ACTION,
    GAME_OVER,
    NIGHT
}

enum class GameState {
    WAITING,
    RUNNING,
    FINISHED
}

enum class Role {
    MAFIOSO,
    PAESANO,
    ISPETTORE,
    SGARRISTA,
    IL_PRETE
}

class Player {
    var name: String = ""
    var role: Role? = null
    var isAlive: Boolean = true

    fun getRole(): Role? {
        return role
    }

    fun die() {
        isAlive = false
    }
}

class GameModel {
    // Attributes
    private val players: MutableList<Player> = ArrayList()
    private var currentPhase: GamePhase = GamePhase.LOBBY
    private var gameState: GameState = GameState.WAITING

    // Attributes necessary for the game
    var roomId: String = ""
    var currentPlayerRole: Role? = null
    var currentPlayerId: String = ""

    fun getPlayers(): List<Player> {
        return players.toList()
    }

    fun getCurrentPhase(): GamePhase {
        return currentPhase
    }

    fun updateGameState(newState: GameState) {
        gameState = newState
    }


    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun removePlayer(playerName: String) {
        players.removeIf { it.name == playerName }
    }

    fun getPlayerByName(name: String): Player? {
        return players.find { it.name == name }
    }

    fun getLivingPlayers(): List<Player> {
        return players.filter { it.isAlive }
    }

    fun getMafiosi(): List<Player> {
        return players.filter { it.role == Role.MAFIOSO && it.isAlive }
    }

    fun getCitizens(): List<Player> {
        return players.filter { it.role != Role.MAFIOSO && it.isAlive }
    }

    fun setPhase(phase: GamePhase) {
        currentPhase = phase
    }

    fun isGameOver(): Boolean {
        return gameState == GameState.FINISHED
    }

    // Method to update model based on server status
    fun updateFromServer(serverGameState: Any) {
        // TODO Implement update based on data received by server
        // Based on data exchanged with server
    }
}
