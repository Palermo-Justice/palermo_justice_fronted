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

// TODO: In the databse schema we also have to implement. The current round. Therefore we should extend the GamePhase

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
    var confirmed: Boolean = false
    var voted: Boolean = false
    var isVoted: Int = 0

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
    // Attributes - main source of truth for players
    private val votes = mutableMapOf<String, String?>()
    private val players: MutableList<Player> = ArrayList()
    private var currentPhase: GamePhase = GamePhase.LOBBY
    private var gameStatus: GameStatus = GameStatus.WAITING

    // Attributes necessary for the game
    var roomId: String = ""
    var currentPlayerRole: Role? = null
    var currentPlayerId: String = ""

    fun registerVote(voterId: String, targetId: String?) {
        votes[voterId] = targetId
    }

    fun getVotes(): Map<String, String?> {
        return votes.toMap()
    }

    fun haveAllVoted(): Boolean {
        val livingPlayers = getLivingPlayers()
        return livingPlayers.all { votes.containsKey(it.id) }
    }

    fun resetVotes() {
        votes.clear()
    }

    fun countVotes(): Pair<Player?, Boolean> {
        val voteCounts = mutableMapOf<String, Int>()

        votes.forEach { (_, targetId) ->
            if (targetId != null) {
                val currentCount = voteCounts[targetId] ?: 0
                voteCounts[targetId] = currentCount + 1
            }
        }

        if (voteCounts.isEmpty()) {
            return Pair(null, false)
        }

        val maxVotes = voteCounts.maxByOrNull { it.value }?.value ?: 0

        val playersWithMaxVotes = voteCounts.filter { it.value == maxVotes }.keys

        if (playersWithMaxVotes.size > 1) {
            return Pair(null, true)
        } else {
            val targetPlayerId = playersWithMaxVotes.first()
            val targetPlayer = getPlayers().find { it.id == targetPlayerId }

            return Pair(targetPlayer, false)
        }
    }

    fun getPlayerVote(playerId: String): String? {
        return votes[playerId]
    }

    // Player management methods
    fun getPlayers(): List<Player> {
        return players.toList()
    }

    // Add a new method to add a player by name
    fun addPlayerByName(playerName: String): Player {
        // First check if player already exists
        val existingPlayer = players.find { it.name == playerName }
        if (existingPlayer != null) {
            return existingPlayer
        }

        // Create new player if not found
        val player = Player(
            id = System.currentTimeMillis().toString(), // Create a unique ID
            name = playerName
        )
        players.add(player)
        return player
    }

    // Method to get only player names (for LobbyScreen)
    fun getPlayerNames(): List<String> {
        return players.map { it.name }
    }

    fun getCurrentPhase(): GamePhase {
        return currentPhase
    }

    fun updateGameState(newState: GameStatus) {
        gameStatus = newState
    }

    fun addPlayer(player: Player) {
        // Check if the player already exists
        if (players.none { it.name == player.name }) {
            players.add(player)
        }
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

    // Methods to get players by role - using the Role class
    fun getPlayersByRole(role: Role): List<Player> {
        return players.filter { it.role == role && it.isAlive }
    }

    fun getMafiosi(): List<Player> {
        return players.filter { it.role is Mafioso && it.isAlive }
    }

    fun getCitizens(): List<Player> {
        return players.filter { it.role !is Mafioso && it.isAlive }
    }

    fun setPhase(phase: GamePhase) {
        currentPhase = phase
    }

    fun isGameOver(): Boolean {
        return gameStatus == GameStatus.FINISHED
    }

    // Method to clear all players (useful for resetting game state)
    fun clearPlayers() {
        players.clear()
    }

    // Method to assign roles to players
    fun assignRoles() {
        // Create a list of roles based on number of players
        val roleList = mutableListOf<Role>()

        // Always have at least one Mafioso
        roleList.add(Mafioso())
        roleList.add(Ispettore())
        roleList.add(Paesano())

        // Fill remaining slots with Paesanos
        while (roleList.size < players.size) {
            roleList.add(Paesano())
        }

        // Shuffle roles and assign
        roleList.shuffle()

        for ((index, player) in players.withIndex()) {
            player.role = roleList[index]
            println("${player.name} assigned role: ${player.role?.name}")
        }
    }

    // Method to update model based on server data
    fun updateFromServer(serverData: Map<String, Any>) {
        // Update room ID if present
        (serverData["roomId"] as? String)?.let { this.roomId = it }

        // Update game status if present
        (serverData["state"] as? String)?.let {
            when (it) {
                "WAITING" -> this.gameStatus = GameStatus.WAITING
                "RUNNING" -> this.gameStatus = GameStatus.RUNNING
                "FINISHED" -> this.gameStatus = GameStatus.FINISHED
            }
        }

        // Update game phase if present
        (serverData["currentPhase"] as? String)?.let {
            try {
                this.currentPhase = GamePhase.valueOf(it)
            } catch (e: Exception) {
                // Phase string doesn't match enum, ignore
            }
        }

        // Update players if present
        (serverData["players"] as? Map<*, *>)?.let { playersMap ->
            // Process each player entry from server
            playersMap.forEach { (playerId, playerData) ->
                if (playerData is Map<*, *>) {
                    val name = playerData["name"] as? String ?: return@forEach
                    val roleName = playerData["role"] as? String
                    val isAlive = playerData["isAlive"] as? Boolean ?: true
                    val isProtected = playerData["isProtected"] as? Boolean ?: false

                    // Find existing player or create new one
                    var player = getPlayerByName(name)
                    if (player == null) {


                        player = Player(
                            id = playerId.toString(),
                            name = name
                        )
                        players.add(player)
                    }

                    // Update player data
                    player.isAlive = isAlive
                    player.isProtected = isProtected

                    // Try to parse the role
                    if (roleName != null) {
                        player.role = when (roleName) {
                            "Mafioso" -> Mafioso()
                            "Ispettore" -> Ispettore()
                            "Sgarrista" -> Sgarrista()
                            "Paesano" -> Paesano()
                            else -> Paesano() // Default role
                        }
                    }
                }
            }
        }
    }
}
