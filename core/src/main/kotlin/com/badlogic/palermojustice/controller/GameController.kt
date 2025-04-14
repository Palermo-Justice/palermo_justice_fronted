package com.badlogic.palermojustice.controller

import com.badlogic.palermojustice.model.*
import com.badlogic.palermojustice.view.GameScreen
import com.badlogic.palermojustice.firebase.FirebaseInterface

/**
 * Main controller for the game.
 * Acts as a central point to access and modify the game model.
 */
class GameController private constructor() {
    val model: GameModel = GameModel()
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
     * Handle user input and delegate to NetworkController
     */
    fun handleInput() {
        // Handle user input and delegate to NetworkController
    }

    /**
     * Start a new game
     */
    fun startGame() {
        // Send start request to server
        // Make sure networkController is initialized before using it
        if (::networkController.isInitialized) {
            // Prepare game state data
            val playerNames = model.getPlayerNames()
            val gameData = mapOf(
                "state" to "RUNNING",
                "currentPhase" to "STARTING",
                "playerList" to playerNames
            )

            // Send the start game message
            networkController.sendMessage("START_GAME", gameData)
        }
    }

    /**
     * Start the night phase
     */
    fun startNightPhase() {
        // Update model
        model.setPhase(GamePhase.NIGHT)

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
    }

    /**
     * Update game state with new data from server
     */
    fun updateGameState(gameData: Map<String, Any>) {
        // Update model with new state from server
        model.updateFromServer(gameData)

        // Update view if initialized
        if (::view.isInitialized) {
            view.updateDisplay()
        }
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
}
