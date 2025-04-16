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
    CHAT_MESSAGE,
    PLAYER_UPDATE,
    CONFIRMATION
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

                    // Forward the raw payload to GameController
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val gameData = message.payload as? Map<String, Any>
                        if (gameData != null) {
                            gameController.updateGameState(gameData)
                        }
                    } catch (e: Exception) {
                        println("Error processing game data: ${e.message}")
                    }
                } catch (e: Exception) {
                    println("Error in routeMessage (GAME_STATE_UPDATE): ${e.message}")
                    e.printStackTrace()
                }
            }
            MessageType.START_GAME -> {
                // Handle start game message
                println("START_GAME message received. Processing...")
                try {
                    @Suppress("UNCHECKED_CAST")
                    val gameData = message.payload as? Map<String, Any>
                    if (gameData != null) {
                        // Update game controller with the received data
                        gameController.updateGameState(gameData)
                    }
                } catch (e: Exception) {
                    println("Error processing START_GAME message: ${e.message}")
                }
            }
            MessageType.JOIN_ROOM -> {
                // Basic implementation to avoid exception
                println("JOIN_ROOM message received. This is not fully implemented yet.")
            }
            MessageType.LEAVE_ROOM -> {
                // Basic implementation to avoid exception
                println("LEAVE_ROOM message received. This is not fully implemented yet.")
            }
            MessageType.PLAYER_ACTION -> {
                // Basic implementation to avoid exception
                println("PLAYER_ACTION message received. This is not fully implemented yet.")
            }
            MessageType.VOTE -> {
                // Basic implementation to avoid exception
                println("VOTE message received. This is not fully implemented yet.")
            }
            MessageType.ROLE_ASSIGNMENT -> {
                // Basic implementation to avoid exception
                println("ROLE_ASSIGNMENT message received. This is not fully implemented yet.")
            }
            MessageType.CHAT_MESSAGE -> {
                println("CHAT_MESSAGE message received. This is not fully implemented yet.")
            }
            MessageType.PLAYER_UPDATE -> {
                println("PLAYER_UPDATE message received. Processing...")
                try {
                    @Suppress("UNCHECKED_CAST")
                    val updateData = message.payload as? Map<String, Any>
                    val playerId = updateData?.get("playerId") as? String
                    val updates = updateData?.get("updates") as? Map<String, Any>

                    if (playerId != null && updates != null && gameController != null) {
                        val player = gameController.model.getPlayers().find { it.id == playerId }
                        if (player != null) {
                            if (updates.containsKey("confirmed")) {
                                val confirmed = updates["confirmed"] as? Boolean ?: false
                                player.confirmed = confirmed
                                println("Player $playerId confirmed status updated to $confirmed")
                            }


                            gameController.updateGameState(mapOf("playerUpdated" to playerId))
                        } else {
                            println("Player with ID $playerId not found in the model")
                        }
                    }
                } catch (e: Exception) {
                    println("Error processing PLAYER_UPDATE message: ${e.message}")
                }
            }

            MessageType.CONFIRMATION -> TODO()
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
