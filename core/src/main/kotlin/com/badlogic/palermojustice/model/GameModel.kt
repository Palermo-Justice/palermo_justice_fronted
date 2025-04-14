package com.badlogic.palermojustice.model
import com.badlogic.palermojustice.model.Role
import java.util.ArrayList

enum class GamePhase {
    LOBBY,
    DAY_DISCUSSION,
    DAY_VOTING,
    NIGHT_ACTION,
    GAME_OVER,
    NIGHT
}

enum class GameStatus {
    WAITING,
    RUNNING,
    FINISHED
}

class Player {
    var id: String = ""
    var name: String = ""
    var role: Role? = null
    var isAlive: Boolean = true
    var isProtected: Boolean = false

    constructor()

    constructor(id: String, name: String, role: Role? = null) {
        this.id = id
        this.name = name
        this.role = role
    }

    fun die() {
        isAlive = false
    }
}

class GameModel {
    // Attributes
    private val players: MutableList<Player> = ArrayList()
    private var currentPhase: GamePhase = GamePhase.LOBBY
    private var gameStatus: GameStatus = GameStatus.WAITING

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

    fun updateGameState(newState: GameStatus) {
        gameStatus = newState
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

//    fun getMafiosi(): List<Player> {
//        return players.filter { it.role == Mafioso && it.isAlive }
//    }
//
//    fun getCitizens(): List<Player> {
//        return players.filter { it.role != Mafioso && it.isAlive }
//    }

    fun setPhase(phase: GamePhase) {
        currentPhase = phase
    }

    fun isGameOver(): Boolean {
        return gameStatus == GameStatus.FINISHED
    }

    // Method to update model based on server status
    fun updateFromServer(serverGameState: Any) {
        // TODO Implement update based on data received by server
        // Based on data exchanged with server
    }
}
