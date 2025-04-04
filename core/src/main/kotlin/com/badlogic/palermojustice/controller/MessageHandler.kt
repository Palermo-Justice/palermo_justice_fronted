package com.badlogic.palermojustice.controller

import com.badlogic.gdx.utils.Json
import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Player
import com.badlogic.palermojustice.model.Role

enum class MessageType {
    JOIN_ROOM,
    LEAVE_ROOM,
    START_GAME,
    PLAYER_ACTION,
    GAME_STATE_UPDATE,
    VOTE,
    ROLE_ASSIGNMENT
}

data class GameMessage(
    val type: MessageType,
    val payload: Any
)

class MessageHandler {
    private val json = Json()
    private val gameController = GameController.getInstance()

    fun encodeMessage(type: MessageType, payload: Any): ByteArray {
        val message = GameMessage(type, payload)
        return json.toJson(message).toByteArray()
    }

    fun decodeMessage(bytes: ByteArray): GameMessage {
        val jsonString = String(bytes)
        return json.fromJson(GameMessage::class.java, jsonString)
    }

    fun routeMessage(message: GameMessage) {
        when (message.type) {
            MessageType.GAME_STATE_UPDATE -> {
                // Update model with new status
                val gameState = json.fromJson(GameState::class.java, json.toJson(message.payload))
                gameController.updateGameState(gameState)
            }
            MessageType.ROLE_ASSIGNMENT -> {
                // Assign role to player
                val role = json.fromJson(Role::class.java, json.toJson(message.payload))
                gameController.assignRole(role)
            }
            // Manage other messages if needed
            else -> {
                // Manage exceptions
            }
        }
    }
}
