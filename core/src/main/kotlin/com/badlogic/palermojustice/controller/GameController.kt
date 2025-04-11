package com.badlogic.palermojustice.controller

import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GamePhase
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Role
import com.badlogic.palermojustice.view.GameScreen
import com.badlogic.palermojustice.firebase.FirebaseInterface

class GameController private constructor() {
    val model: GameModel = GameModel()
    lateinit var view: GameScreen
    private lateinit var networkController: FirebaseInterface

    companion object {
        @Volatile
        private var instance: GameController? = null

        // Get singleton instance of GameController
        fun getInstance(): GameController {
            return instance ?: synchronized(this) {
                instance ?: GameController().also { instance = it }
            }
        }
    }

    // Method to set the network controller after instance creation
    fun setNetworkController(controller: FirebaseInterface) {
        if (!::networkController.isInitialized) {
            networkController = controller
        }
    }

    fun handleInput() {
        // Handle user input and delegate to NetworkController
    }

    fun startGame() {
        // Send start request to server
        // Make sure networkController is initialized before using it
        if (::networkController.isInitialized) {
            // Code to send messages to server
        }
    }

    fun startNightPhase() {
        // Update view for night phase
        view.updatePhaseDisplay(GamePhase.NIGHT.toString())
    }

    fun updateGameState(gameState: GameState) {
        // Update model with new state
        model.updateGameState(gameState)
        // Update view
        view.updateDisplay()
    }

    fun assignRole(role: Role) {
        // Assign role to current player
        model.currentPlayerRole = role
        //view.showRoleAssignment(GameModel)
        //TODO correct this line - to understand how
    }

    fun vote(targetPlayerId: String) {
        // Send vote to server
        if (::networkController.isInitialized) {
            // Code to send vote messages to server
        }
    }

    fun performRoleAction(actionType: String, targetId: String) {
        // Send role action to server
        if (::networkController.isInitialized) {
            // Code to send role actions to server
        }
    }
}
