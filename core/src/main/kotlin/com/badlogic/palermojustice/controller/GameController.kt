package com.badlogic.palermojustice.controller

import com.badlogic.gdx.Gdx
import com.badlogic.palermojustice.model.*
import com.badlogic.palermojustice.view.GameScreen
import com.badlogic.palermojustice.firebase.FirebaseInterface

/**
 * Main controller for the game.
 * Acts as a central point to access and modify the game model.
 */
class GameController private constructor() {
    val model: GameModel = GameModel()
    private var messageHandler: MessageHandler? = null
    lateinit var view: GameScreen
    private lateinit var networkController: FirebaseInterface

    companion object {
        @Volatile
        private var instance: GameController? = null

        /**
         * Get singleton instance of GameController
         */
        fun getInstance(): GameController {
            return instance ?: synchronized(this) {
                instance ?: GameController().also { instance = it }
            }
        }
    }

    /**
     * Method to set the network controller after instance creation
     */
    fun setNetworkController(controller: FirebaseInterface) {
        if (!::networkController.isInitialized) {
            networkController = controller
        }
    }

    /**
     * Method to get the message handler
     */
    fun getMessageHandler(): MessageHandler? {
        if (messageHandler == null) {
            messageHandler = MessageHandler()
        }
        return messageHandler
    }

    /**
     * Handle user input and delegate to NetworkController
     */
    fun handleInput() {
        // Handle user input and delegate to NetworkController
    }

    /**
     * Start a new game
     */
    fun startGame() {
        // First assign roles to all players
        model.assignRoles()

        // Prepare player data with their assigned roles
        val playersData = model.getPlayers().associate { player ->
            player.id to mapOf(
                "name" to player.name,
                "role" to (player.role?.name ?: "Paesano"),
                "isAlive" to player.isAlive,
                "isProtected" to player.isProtected,
                "confirmed" to false  // Initialize confirmed to false for all players
            )
        }

        // Prepare complete game data
        val gameData = mapOf(
            "state" to "RUNNING",
            "currentPhase" to "STARTING",
            "players" to playersData  // Include complete player data with roles
        )

        // Send the start game message with all data
        if (::networkController.isInitialized) {
            networkController.sendMessage("START_GAME", gameData)

            // Also send a game state update to ensure synchronization
            networkController.sendMessage("GAME_STATE_UPDATE", gameData)
        }
    }

    /**
     * Start the night phase
     */
    fun startNightPhase() {
        // Update model
        model.setPhase(GamePhase.NIGHT)

        // Reset confirmed status for all players
        val playersData = model.getPlayers().associate { player ->
            player.confirmed = false // Reset local confirmed state
            player.id to mapOf(
                "confirmed" to false
            )
        }

        // Update phase in Firebase
        if (::networkController.isInitialized) {
            networkController.sendMessage("GAME_STATE_UPDATE", mapOf(
                "currentPhase" to "NIGHT",
                "players" to playersData  // Reset player confirmed status
            ))
        }

        // Update view for night phase
        if (::view.isInitialized) {
            view.updatePhaseDisplay(GamePhase.NIGHT.toString())
        }

        // Reset night sequence for new night
        resetNightSequence()
    }

    /**
     * Reset night sequence
     */
    private fun resetNightSequence() {
        // Can implement night sequence reset logic here
        // Or defer to a helper function
        GameStateHelper.resetNightSequence()
    }

    /**
     * Update game state with new data from server
     */
    fun updateGameState(gameData: Map<String, Any>) {
        // Se il messaggaio è in fase di elaborazione di un'azione di ruolo (RoleActionScreen)
        // e l'aggiornamento riguarda solo le conferme, ignora l'aggiornamento completo
        if (messageHandler?.isInRoleAction() == true && isConfirmationOnly(gameData)) {
            Gdx.app.log("GameController", "Ignoring confirmation-only update during role action phase")
            return
        }

        // Update model with new state from server
        model.updateFromServer(gameData)

        // Update view if initialized
        if (::view.isInitialized) {
            Gdx.app.postRunnable {
                view.updateDisplay()
            }
        }
    }

    /**
     * Verifica se l'aggiornamento riguarda solo conferme dei giocatori
     */
    private fun isConfirmationOnly(gameData: Map<String, Any>): Boolean {
        // Controlla la fase corrente
        val phase = gameData["currentPhase"] as? String
        if (phase != "NIGHT" && phase != "NIGHT_ACTION") {
            return false
        }

        // Controlla se ci sono solo aggiornamenti di "confirmed"
        val playersMap = gameData["players"] as? Map<String, Any> ?: return false

        for ((_, playerData) in playersMap) {
            if (playerData is Map<*, *>) {
                // Se contiene altre proprietà oltre a "confirmed" e "name"
                if (playerData.size > 2 &&
                    (playerData.containsKey("isAlive") ||
                        playerData.containsKey("role") ||
                        playerData.containsKey("isProtected"))) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Assign a role to the current player
     */
    fun assignRole(role: Role) {
        // Assign role to current player
        model.currentPlayerRole = role

        // Update view with the new role if available
        if (::view.isInitialized) {
            view.showRoleAssignment(role.name)
        }
    }

    /**
     * Send a vote for a player
     */
    fun vote(targetPlayerId: String) {
        // Send vote to server
        Gdx.app.log("GameController", "::networkcontroller.isInitialized: ${::networkController.isInitialized}")
        if (::networkController.isInitialized) {
            val voteData = mapOf(
                "type" to "VOTE",
                "targetId" to targetPlayerId,
                "voterId" to model.currentPlayerId
            )

            networkController.sendMessage("VOTE", voteData)
        }
    }

    /**
     * Perform a role-specific action
     */
    fun performRoleAction(actionType: String, targetId: String) {
        // Send role action to server
        if (::networkController.isInitialized) {
            val actionData = mapOf(
                "type" to actionType,
                "targetId" to targetId,
                "actorId" to model.currentPlayerId,
                "role" to (model.currentPlayerRole?.name ?: "UNKNOWN")
            )

            networkController.sendMessage("PLAYER_ACTION", actionData)
        }
    }

    /**
     * Process game messages received from server
     */
    fun processGameMessage(message: GameMessage) {
        when (message.type) {
            MessageType.GAME_STATE_UPDATE -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val gameData = message.payload as? Map<String, Any>
                    if (gameData != null) {
                        updateGameState(gameData)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            MessageType.ROLE_ASSIGNMENT -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val roleData = message.payload as? Map<String, Any>
                    val roleName = roleData?.get("role") as? String

                    if (roleName != null) {
                        // Create the appropriate role instance
                        val role = when (roleName) {
                            "Mafioso" -> Mafioso()
                            "Ispettore" -> Ispettore()
                            "Sgarrista" -> Sgarrista()
                            "Paesano" -> Paesano()
                            else -> Paesano() // Default role
                        }
                        assignRole(role)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Handle other message types
            else -> {
                // Process other types of messages
            }
        }
    }

    /**
     * Send confirmation that the player has completed their night action
     * This is specialized to only send the confirmation and not trigger a full game state update
     */
    fun sendConfirmation(playerId: String, rolePhase: String) {
        if (::networkController.isInitialized) {
            // Send only the targeted update
            val confirmationData = mapOf(
                "playerId" to playerId,
                "rolePhase" to rolePhase,
                "confirmed" to true
            )

            // Segnala che stiamo processando un'azione di ruolo
            if (messageHandler == null) {
                messageHandler = MessageHandler()
            }
            messageHandler?.setProcessingRoleAction(true)

            // Use a dedicated message type to avoid full game updates
            networkController.sendMessage("CONFIRMATION", confirmationData)

            Gdx.app.log("GameController", "Sent confirmation for player $playerId in phase $rolePhase")
        }
    }

    /**
     * Reset confirmation status for all players
     */
    fun resetConfirmations() {
        val players = model.getPlayers()

        // Reset locally
        players.forEach { it.confirmed = false }

        // Create reset data
        val resetData = players.associate { player ->
            player.id to mapOf("confirmed" to false)
        }

        if (::networkController.isInitialized) {
            networkController.sendMessage("RESET_CONFIRMATIONS", mapOf(
                "players" to resetData
            ))
        }
    }
}
