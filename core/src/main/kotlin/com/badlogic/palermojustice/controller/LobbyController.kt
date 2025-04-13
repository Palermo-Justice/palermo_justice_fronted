package com.badlogic.palermojustice.controller

import com.badlogic.gdx.Gdx
import com.badlogic.palermojustice.firebase.FirebaseInterface
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

        // Start listening for updates immediately
        setupPlayerUpdates()
    }

    // Method to connect the view to the controller
    fun setView(lobbyScreen: LobbyScreen) {
        this.view = lobbyScreen

        // Initial update of the view with current player list - on the render thread
        Gdx.app.postRunnable {
            view?.updatePlayersList(playersList.toList())
        }
    }

    // Configure player updates
    private fun setupPlayerUpdates() {
        // Listen for game updates from Firebase
        networkController.listenForGameUpdates { gameData ->
            Gdx.app.postRunnable {
                updatePlayersFromGameData(gameData)
            }
        }

        // Also get initial room info
        networkController.getRoomInfo(roomId) { roomData ->
            if (roomData != null) {
                Gdx.app.postRunnable {
                    updatePlayersFromGameData(roomData)
                }
            }
        }
    }

    // Update player list from game data
    private fun updatePlayersFromGameData(gameData: Map<String, Any>) {
        try {
            // Extract player data - with the new structure, players are directly under the room
            val playersMap = gameData["players"] as? Map<*, *> ?: return

            // Make a temporary list to collect players
            val tempPlayersList = mutableListOf<String>()

            // Process each player entry
            playersMap.forEach { (_, playerData) ->
                try {
                    when (playerData) {
                        is Map<*, *> -> {
                            // In the new structure, playerData should always be a Map
                            // with 'name' as one of the fields
                            val name = playerData["name"] as? String
                            if (name != null && !tempPlayersList.contains(name)) {
                                tempPlayersList.add(name)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue with next player
                }
            }

            // If no players were found using the map approach, check if the host name is available
            if (tempPlayersList.isEmpty()) {
                val hostName = gameData["hostName"] as? String
                if (hostName != null && !tempPlayersList.contains(hostName)) {
                    tempPlayersList.add(hostName)
                }
            }

            // Ensure current player is in the list
            if (!tempPlayersList.contains(playerName)) {
                tempPlayersList.add(playerName)
            }

            // Update the actual player list
            playersList.clear()
            playersList.addAll(tempPlayersList)

            // Update UI if view is available - always on render thread
            Gdx.app.postRunnable {
                view?.updatePlayersList(playersList.toList())
            }
        } catch (e: Exception) {
            // Ensure player is still in the list if something goes wrong
            if (!playersList.contains(playerName)) {
                playersList.add(playerName)
            }
            Gdx.app.postRunnable {
                view?.updatePlayersList(playersList.toList())
            }
        }
    }

    // Join an existing room with validation
    fun joinRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        // First check if the room exists
        networkController.getRoomInfo(roomId) { roomData ->
            if (roomData == null) {
                // Room doesn't exist
                callback(false)
                return@getRoomInfo
            }

            // Room exists, check if it's in a joinable state
            val state = roomData["state"] as? String
            if (state != null && state != "WAITING" && state != "LOBBY") {
                // Room is not in a joinable state (game already started)
                callback(false)
                return@getRoomInfo
            }

            // Check if the room is full
            val settings = roomData["settings"] as? Map<*, *>
            val maxPlayers = settings?.get("maxPlayers") as? Number ?: 5
            val players = roomData["players"] as? Map<*, *> ?: mapOf<String, Any>()

            if (players.size >= maxPlayers.toInt()) {
                // Room is full
                callback(false)
                return@getRoomInfo
            }

            // Room exists and is joinable, so connect to it
            networkController.connectToRoom(roomId, playerName, callback)
        }
    }

    // Start the game
    fun startGame() {
        if (isHost && playersList.size >= 3) { // Minimum 3 players to start
            // Send start game message
            val gameData = mapOf(
                "state" to "RUNNING",
                "currentPhase" to "STARTING",
                "playerList" to playersList // Store a simple list of player names for easy access
            )
            networkController.sendMessage("START_GAME", gameData)
            view?.navigateToGameScreen(roomId, playerName, isHost)
        } else if (playersList.size < 3) {
            view?.showMessage("Need at least 3 players to start")
        } else if (!isHost) {
            view?.showMessage("Only the host can start the game")
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

    // Copy room code to clipboard - platform specific implementation required
    fun copyRoomCode(): String {
        return roomId
    }
}
