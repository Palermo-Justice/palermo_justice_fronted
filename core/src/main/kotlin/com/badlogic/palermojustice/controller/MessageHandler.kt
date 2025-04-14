package com.badlogic.palermojustice.controller

import com.badlogic.gdx.utils.Json
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
        // Log the message type for debugging
        println("Routing message of type: ${message.type}")

        when (message.type) {
            MessageType.GAME_STATE_UPDATE -> {
                try {
                    println("Handling GAME_STATE_UPDATE. Payload: ${message.payload}")

                    val payloadJson = json.toJson(message.payload)
                    println("Payload JSON: $payloadJson")

                    if (gameController == null) {
                        println("GameController is null!")
                        return
                    }

                    // Here we're just logging instead of trying to parse the GameState
                    // Until full implementation is done
                    println("GAME_STATE_UPDATE received with payload: $payloadJson")

                    // Instead of trying to parse it to GameState directly, we'll just forward
                    // the callback for now
                } catch (e: Exception) {
                    println("Error in routeMessage (GAME_STATE_UPDATE): ${e.message}")
                    e.printStackTrace()
                }
            }
            MessageType.JOIN_ROOM -> {
                // Basic implementation to avoid TODO() exception
                println("JOIN_ROOM message received. This is not fully implemented yet.")
                // Actual implementation would handle player joining a room
            }
            MessageType.LEAVE_ROOM -> {
                // Basic implementation to avoid TODO() exception
                println("LEAVE_ROOM message received. This is not fully implemented yet.")
                // Actual implementation would handle player leaving a room
            }
            MessageType.START_GAME -> {
                // Basic implementation to avoid TODO() exception
                println("START_GAME message received. This is not fully implemented yet.")
                // Actual implementation would handle game start logic
            }
            MessageType.PLAYER_ACTION -> {
                // Basic implementation to avoid TODO() exception
                println("PLAYER_ACTION message received. This is not fully implemented yet.")
                // Actual implementation would handle player actions during the game
            }
            MessageType.VOTE -> {
                // Basic implementation to avoid TODO() exception
                println("VOTE message received. This is not fully implemented yet.")
                // Actual implementation would handle voting logic
            }
            MessageType.ROLE_ASSIGNMENT -> {
                // Basic implementation to avoid TODO() exception
                println("ROLE_ASSIGNMENT message received. This is not fully implemented yet.")
                // Actual implementation would handle role assignment
            }
            MessageType.CHAT_MESSAGE -> {
                // Basic implementation to avoid TODO() exception
                println("CHAT_MESSAGE message received. This is not fully implemented yet.")
                // Actual implementation would handle chat messages
            }
        }

        // Call registered callbacks for this message type
        callbacks[message.type]?.forEach { callback ->
            try {
                callback(message)
            } catch (e: Exception) {
                println("Error executing callback: ${e.message}")
                e.printStackTrace()
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
