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

    // Flag per prevenire aggiornamenti di stato durante la fase di azione dei ruoli
    private var processingRoleAction = false

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

                    // Se siamo in fase di azione dei ruoli e questo è un aggiornamento di stato
                    // che include solo le conferme, gestiamo diversamente
                    if (processingRoleAction && isRoleConfirmationUpdate(message.payload)) {
                        println("Detected role confirmation update, handling separately")
                        // Aggiorna solo le conferme senza riavviare l'intera schermata
                        handleRoleConfirmationsOnly(message.payload)

                        // Notifica solo i callback di tipo CONFIRMATION invece di tutti i GAME_STATE_UPDATE
                        callbacks[MessageType.CONFIRMATION]?.forEach { callback ->
                            try {
                                callback(GameMessage(MessageType.CONFIRMATION, message.payload))
                            } catch (e: Exception) {
                                println("Error executing CONFIRMATION callback: ${e.message}")
                            }
                        }

                        // Non proseguire con l'aggiornamento completo del game state
                        return
                    }

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
            MessageType.CONFIRMATION -> {
                // Gestisce i messaggi di conferma senza scatenare un aggiornamento completo
                println("CONFIRMATION message received. Handling player confirmations.")
                try {
                    @Suppress("UNCHECKED_CAST")
                    val confirmationData = message.payload as? Map<String, Any>
                    if (confirmationData != null) {
                        handleRoleConfirmationsOnly(confirmationData)
                    }
                } catch (e: Exception) {
                    println("Error processing CONFIRMATION message: ${e.message}")
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

                            // Non aggiornare l'UI per i singoli aggiornamenti di conferma
                            if (!isRoleConfirmationUpdate(updateData)) {
                                gameController.updateGameState(mapOf("playerUpdated" to playerId))
                            }
                        } else {
                            println("Player with ID $playerId not found in the model")
                        }
                    }
                } catch (e: Exception) {
                    println("Error processing PLAYER_UPDATE message: ${e.message}")
                }
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

    /**
     * Verifica se l'aggiornamento riguarda solo le conferme durante la fase di azione dei ruoli
     */
    private fun isRoleConfirmationUpdate(payload: Any): Boolean {
        try {
            @Suppress("UNCHECKED_CAST")
            val data = payload as? Map<String, Any> ?: return false

            // Verifica se è presente il nodo confirmations
            if (data.containsKey("confirmations")) {
                return true
            }

            // Verifica se è un aggiornamento di un singolo giocatore con "confirmed"
            if (data.containsKey("playerId") && data.containsKey("updates")) {
                val updates = data["updates"] as? Map<String, Any> ?: return false
                return updates.containsKey("confirmed")
            }

            // Verifica se è un aggiornamento completo con "players" che contiene "confirmed"
            if (data.containsKey("players")) {
                val players = data["players"] as? Map<String, Any> ?: return false
                for (playerData in players.values) {
                    if (playerData is Map<*, *> && playerData.containsKey("confirmed")) {
                        return true
                    }
                }
            }

            return false
        } catch (e: Exception) {
            println("Error checking if update is a confirmation: ${e.message}")
            return false
        }
    }

    /**
     * Gestisce solo gli aggiornamenti delle conferme senza riavviare l'intera schermata
     */
    private fun handleRoleConfirmationsOnly(payload: Any) {
        try {
            @Suppress("UNCHECKED_CAST")
            val data = payload as? Map<String, Any> ?: return

            // Se contiene il nodo confirmations, aggiorna solo le conferme
            if (data.containsKey("confirmations")) {
                val confirmations = data["confirmations"] as? Map<String, Boolean> ?: return

                // Aggiorna lo stato confirmed per i giocatori nel modello
                for ((playerId, confirmed) in confirmations) {
                    val player = gameController.model.getPlayers().find { it.id == playerId }
                    if (player != null) {
                        player.confirmed = confirmed
                        println("Updated player $playerId confirmed status to $confirmed")
                    }
                }
                return
            }

            // Se è un aggiornamento completo con "players", estrai solo le conferme
            if (data.containsKey("players")) {
                val players = data["players"] as? Map<String, Any> ?: return

                for ((playerId, playerData) in players) {
                    if (playerData is Map<*, *>) {
                        val confirmed = playerData["confirmed"] as? Boolean
                        if (confirmed != null) {
                            val player = gameController.model.getPlayers().find { it.id == playerId }
                            if (player != null) {
                                player.confirmed = confirmed
                                println("Updated player $playerId confirmed status to $confirmed")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling role confirmations: ${e.message}")
        }
    }

    /**
     * Imposta il flag per indicare che siamo in fase di azione dei ruoli
     */
    fun setProcessingRoleAction(processing: Boolean) {
        processingRoleAction = processing
        println("ProcessingRoleAction set to $processing")
    }

    /**
     * Verifica se siamo in fase di azione dei ruoli
     */
    fun isInRoleAction(): Boolean {
        return processingRoleAction
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
