package com.badlogic.palermojustice.controller

import com.badlogic.gdx.Gdx
import com.badlogic.palermojustice.firebase.FirebaseInterface
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.view.LobbyScreen

class LobbyController(
    private val networkController: FirebaseInterface,
    private val roomId: String,
    private val playerName: String,
    private val isHost: Boolean
) {
    private val messageHandler = MessageHandler()
    private var view: LobbyScreen? = null
    private val playersList = mutableListOf<String>()

    init {
        // Add current player to the list
        playersList.add(playerName)
    }

    // Method to connect the view to the controller
    fun setView(lobbyScreen: LobbyScreen) {
        this.view = lobbyScreen
        setupPlayerUpdates()
    }

    // Configure player updates
    private fun setupPlayerUpdates() {
        // Listen for game updates from Firebase
        networkController.listenForGameUpdates { gameData ->
            Gdx.app.postRunnable {
                updatePlayersFromGameData(gameData)
            }
        }
    }

    // Update player list from game data
    private fun updatePlayersFromGameData(gameData: Map<String, Any>) {
        // Extract player data
        val playersMap = gameData["players"] as? Map<String, Any> ?: return

        // Reset player list (except self)
        playersList.clear()
        playersList.add(playerName) // Keep self in the list

        // Add all players from data
        playersMap.forEach { (_, playerData) ->
            val player = playerData as? Map<String, Any> ?: return@forEach
            val name = player["name"] as? String ?: return@forEach
            if (name != playerName && !playersList.contains(name)) {
                playersList.add(name)
            }
        }

        // Update UI
        view?.updatePlayersList(playersList)
    }

    // Start the game
    fun startGame() {
        if (isHost && playersList.size >= 3) { // Minimum 3 players to start
            // Send start game message
            val gameData = mapOf(
                "state" to "RUNNING",
                "players" to playersList
            )
            networkController.sendMessage("START_GAME", gameData)
            view?.navigateToGameScreen(roomId, playerName, isHost)
        } else if (playersList.size < 3) {
            view?.showMessage("Need at least 3 players to start")
        }
    }

    // Disconnect from the game
    fun disconnect() {
        networkController.disconnect()
    }

    // Get current player list
    fun getPlayersList(): List<String> {
        return playersList.toList() // Return a copy of the list
    }

    // Check if user is host
    fun isHost(): Boolean {
        return isHost
    }

    // Join an existing room
    fun joinRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        networkController.connectToRoom(roomId, playerName, callback)
    }

    // Copy room code to clipboard - platform specific implementation required
    fun copyRoomCode(): String {
        return roomId
    }
}
