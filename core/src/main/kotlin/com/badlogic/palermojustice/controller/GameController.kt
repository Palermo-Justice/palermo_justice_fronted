package com.badlogic.palermojustice.controller

import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GamePhase
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Role
import com.badlogic.palermojustice.view.GameScreen

class GameController private constructor() {
    val model: GameModel = GameModel()
    lateinit var view: GameScreen
    private val networkController = NetworkController.getInstance()

    companion object {
        private var instance: GameController? = null

        fun getInstance(): GameController {
            if (instance == null) {
                instance = GameController()
            }
            return instance!!
        }
    }

    fun handleInput() {
        // Gestisci input utente e delegazione al NetworkController
    }

    fun startGame() {
        // Invia richiesta di avvio al server
        val startMessage = MessageHandler().encodeMessage(
            MessageType.START_GAME,
            mapOf("roomId" to model.roomId)
        )
        networkController.sendMessage(startMessage)
    }

    fun startNightPhase() {
        // Aggiorna la vista per la fase notturna
        view.updatePhaseDisplay(GamePhase.NIGHT)
    }

    fun updateGameState(gameState: GameState) {
        // Aggiorna il modello con il nuovo stato
        model.updateGameState(gameState)
        // Aggiorna la vista
        view.updateDisplay()
    }

    fun assignRole(role: Role) {
        // Assegna ruolo al giocatore corrente
        model.currentPlayerRole = role
        //view.showRoleAssignment(GameModel)
        //TODO correct this line - to understand how
    }

    fun vote(targetPlayerId: String) {
        // Invia voto al server
        val voteMessage = MessageHandler().encodeMessage(
            MessageType.VOTE,
            mapOf("targetId" to targetPlayerId)
        )
        networkController.sendMessage(voteMessage)
    }

    fun performRoleAction(actionType: String, targetId: String) {
        // Invia azione del ruolo al server
        val actionMessage = MessageHandler().encodeMessage(
            MessageType.PLAYER_ACTION,
            mapOf("actionType" to actionType, "targetId" to targetId)
        )
        networkController.sendMessage(actionMessage)
    }
}
