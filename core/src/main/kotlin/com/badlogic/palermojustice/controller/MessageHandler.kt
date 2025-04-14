package com.badlogic.palermojustice.controller

import com.badlogic.gdx.utils.Json
import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GameState
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
                try {
                    println("Payload: ${message.payload}")

                    val payloadJson = json.toJson(message.payload)
                    println("Payload JSON: $payloadJson")

                    if (gameController == null) {
                        println("GameController is null!")
                        return
                    }

                    val gameState = try {
                        json.fromJson(GameState::class.java, payloadJson)
                    } catch (e: Exception) {
                        println("Error converting payload to GameState: ${e.message}")
                        null
                    }

                    if (gameState == null) {
                        println("Converted GameState is null!")
                        return
                    }

                    //gameController.updateGameState(gameState)
                } catch (e: Exception) {
                    println("Error in routeMessage: ${e.message}")
                    e.printStackTrace()
                }
            }
            MessageType.JOIN_ROOM -> TODO()
            MessageType.LEAVE_ROOM -> TODO()
            MessageType.START_GAME -> TODO()
            MessageType.PLAYER_ACTION -> TODO()
            MessageType.VOTE -> TODO()
            MessageType.ROLE_ASSIGNMENT -> TODO()
            MessageType.CHAT_MESSAGE -> TODO()
        }

        callbacks[message.type]?.forEach { callback ->
            try {
                callback(message)
            } catch (e: Exception) {
                println("Error executing callback: ${e.message}")
            }
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
