package com.badlogic.palermojustice.controller

import com.badlogic.gdx.Gdx
import com.badlogic.palermojustice.firebase.FirebaseInterface
import com.badlogic.palermojustice.view.LobbyScreen

/**
 * Controller for the lobby screen.
 * Uses GameModel via GameController as the single source of truth for players.
 */
class LobbyController(
    private val networkController: FirebaseInterface,
    private val roomId: String,
    private val playerName: String,
    private val isHost: Boolean
) {
    private val messageHandler = MessageHandler()
    private var view: LobbyScreen? = null
    private val gameController = GameController.getInstance()

    // Flag per prevenire navigazioni multiple alla GameScreen
    private var hasNavigatedToGameScreen = false

    init {
        // Initialize game state with this room's info
        gameController.model.roomId = roomId

        // Add current player to the model
        gameController.model.addPlayerByName(playerName)

        // Register callback for game state updates
        messageHandler.registerCallback(MessageType.GAME_STATE_UPDATE) { message ->
            try {
                @Suppress("UNCHECKED_CAST")
                val gameData = message.payload as? Map<String, Any>
                if (gameData != null) {
                    Gdx.app.postRunnable {
                        // Verifica lo stato della room prima di aggiornare i giocatori
                        val state = gameData["state"] as? String

                        // Verifica se stiamo nella fase di azione della notte e se ci sono aggiornamenti di conferma
                        val currentPhase = gameData["currentPhase"] as? String
                        val isNightActionPhase = currentPhase == "NIGHT" || currentPhase == "NIGHT_ACTION"

                        // Redirect to game screen if game is starting or running
                        if (state == "RUNNING" && !hasNavigatedToGameScreen) {
                            Gdx.app.log("LobbyController", "Game is running, navigating to game screen")
                            hasNavigatedToGameScreen = true
                            view?.navigateToGameScreen(roomId, playerName, isHost)
                        } else if (state != "RUNNING") {
                            // Regular player list update - solo se non siamo in uno stato 'RUNNING'
                            updatePlayersFromGameData(gameData)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error processing game state update: ${e.message}")
            }
        }

        // Also register for START_GAME messages specifically
        messageHandler.registerCallback(MessageType.START_GAME) { message ->
            Gdx.app.postRunnable {
                // Naviga solo se non abbiamo già navigato
                if (!hasNavigatedToGameScreen) {
                    Gdx.app.log("LobbyController", "START_GAME received, navigating to game screen")
                    hasNavigatedToGameScreen = true
                    // When START_GAME is received, navigate to game screen
                    view?.navigateToGameScreen(roomId, playerName, isHost)
                }
            }
        }

        // Start listening for updates immediately
        setupPlayerUpdates()
    }

    /**
     * Method to connect the view to the controller
     */
    fun setView(lobbyScreen: LobbyScreen) {
        this.view = lobbyScreen

        // Initial update of the view with current player list - on the render thread
        Gdx.app.postRunnable {
            view?.updatePlayersList(gameController.model.getPlayerNames())
        }
    }

    /**
     * Configure player updates
     */
    private fun setupPlayerUpdates() {
        // Listen for game updates from Firebase
        networkController.listenForGameUpdates { gameData ->
            Gdx.app.postRunnable {
                // Check game state before updating player list
                val state = gameData["state"] as? String

                // Naviga solo se il gioco è in esecuzione e non abbiamo già navigato
                if (state == "RUNNING" && !hasNavigatedToGameScreen) {
                    Gdx.app.log("LobbyController", "Game is running from listener, navigating to game screen")
                    hasNavigatedToGameScreen = true
                    view?.navigateToGameScreen(roomId, playerName, isHost)
                } else if (state != "RUNNING") {
                    // Aggiorna la lista giocatori solo se non siamo in stato RUNNING
                    updatePlayersFromGameData(gameData)
                }
            }
        }

        // Also get initial room info
        networkController.getRoomInfo(roomId) { roomData ->
            if (roomData != null) {
                Gdx.app.postRunnable {
                    // Check initial game state
                    val state = roomData["state"] as? String

                    // Naviga solo se il gioco è in esecuzione e non abbiamo già navigato
                    if (state == "RUNNING" && !hasNavigatedToGameScreen) {
                        Gdx.app.log("LobbyController", "Game is running from initial room info, navigating to game screen")
                        hasNavigatedToGameScreen = true
                        view?.navigateToGameScreen(roomId, playerName, isHost)
                    } else if (state != "RUNNING") {
                        // Aggiorna la lista giocatori solo se non siamo in stato RUNNING
                        updatePlayersFromGameData(roomData)
                    }
                }
            }
        }
    }

    /**
     * Update player list from game data
     */
    private fun updatePlayersFromGameData(gameData: Map<String, Any>) {
        try {
            // Extract player data - with the new structure, players are directly under the room
            val playersMap = gameData["players"] as? Map<*, *> ?: return

            // Process players from server data
            playersMap.forEach { (_, playerData) ->
                try {
                    when (playerData) {
                        is Map<*, *> -> {
                            // In the new structure, playerData should always be a Map
                            // with 'name' as one of the fields
                            val name = playerData["name"] as? String
                            if (name != null) {
                                gameController.model.addPlayerByName(name)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue with next player
                }
            }

            // If no players were found using the map approach, check if the host name is available
            if (gameController.model.getPlayers().isEmpty()) {
                val hostName = gameData["hostName"] as? String
                if (hostName != null) {
                    gameController.model.addPlayerByName(hostName)
                }
            }

            // Ensure current player is in the list
            gameController.model.addPlayerByName(playerName)

            // Update UI if view is available - always on render thread
            Gdx.app.postRunnable {
                view?.updatePlayersList(gameController.model.getPlayerNames())
            }
        } catch (e: Exception) {
            // Ensure player is still in the list if something goes wrong
            gameController.model.addPlayerByName(playerName)

            Gdx.app.postRunnable {
                view?.updatePlayersList(gameController.model.getPlayerNames())
            }
        }
    }

    /**
     * Join an existing room with validation
     */
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

    /**
     * Start the game
     */
    fun startGame() {
        val players = gameController.model.getPlayers()
        if (isHost && players.size >= 3) { // Minimum 3 players to start
            // Imposta il flag prima di inviare messaggi per prevenire ricaricamenti multipli
            hasNavigatedToGameScreen = true

            // Assign roles to players
            gameController.model.assignRoles()

            // Prepare player data with their assigned roles
            val playersData = gameController.model.getPlayers().associate { player ->
                player.id to mapOf(
                    "name" to player.name,
                    "role" to (player.role?.name ?: "Paesano"),
                    "isAlive" to player.isAlive,
                    "isProtected" to player.isProtected,
                    "confirmed" to false
                )
            }

            // First update the room state to RUNNING with player roles
            val updateData = mapOf(
                "state" to "RUNNING",
                "currentPhase" to "STARTING",
                "players" to playersData
            )
            networkController.sendMessage("GAME_STATE_UPDATE", updateData)

            // Then send the START_GAME message with full player data
            val gameData = mapOf(
                "state" to "RUNNING",
                "currentPhase" to "STARTING",
                "playerList" to gameController.model.getPlayerNames(),
                "players" to playersData
            )
            networkController.sendMessage("START_GAME", gameData)

            // Navigate host to game screen
            view?.navigateToGameScreen(roomId, playerName, isHost)
        } else if (players.size < 3) {
            view?.showMessage("Need at least 3 players to start")
        } else if (!isHost) {
            view?.showMessage("Only the host can start the game")
        }
    }

    /**
     * Disconnect from the game
     */
    fun disconnect() {
        networkController.disconnect()
    }

    /**
     * Get current player list
     */
    fun getPlayersList(): List<String> {
        return gameController.model.getPlayerNames()
    }

    /**
     * Check if user is host
     */
    fun isHost(): Boolean {
        return isHost
    }

    /**
     * Copy room code to clipboard - platform specific implementation required
     */
    fun copyRoomCode(): String {
        return roomId
    }

    /**
     * Reset navigation state (da usare se necessario)
     */
    fun resetNavigationState() {
        hasNavigatedToGameScreen = false
    }
}
