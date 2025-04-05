package com.badlogic.palermojustice.controller

import com.badlogic.gdx.utils.Json
import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Player
import com.badlogic.palermojustice.model.Role
import java.util.concurrent.ConcurrentHashMap

enum class MessageType {
    JOIN_ROOM,
    LEAVE_ROOM,
    START_GAME,
    PLAYER_ACTION,
    GAME_STATE_UPDATE,
    VOTE,
    ROLE_ASSIGNMENT,
    CHAT_MESSAGE  // Added for the tests
}

data class GameMessage(
    val type: MessageType,
    val payload: Any
)

class MessageHandler {
    val json = Json()
    private val gameController = GameController.getInstance()

    // callback datastructure
    private val callbacks = ConcurrentHashMap<MessageType, MutableList<(GameMessage) -> Unit>>()

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
            else -> {
                // TODO manage exceptions
            }
        }

        callbacks[message.type]?.forEach { callback ->
            callback(message)
        }
    }

    fun registerCallback(type: MessageType, callback: (GameMessage) -> Unit) {
        if (!callbacks.containsKey(type)) {
            callbacks[type] = mutableListOf()
        }

        callbacks[type]?.add(callback)
    }

    fun unregisterCallback(type: MessageType, callback: (GameMessage) -> Unit) {
        callbacks[type]?.remove(callback)
    }

    fun clearCallbacks() {
        callbacks.clear()
    }
}
