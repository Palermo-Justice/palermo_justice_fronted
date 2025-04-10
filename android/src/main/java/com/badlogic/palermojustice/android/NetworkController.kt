package com.badlogic.palermojustice.android

import com.badlogic.palermojustice.controller.GameMessage
import com.badlogic.palermojustice.controller.MessageHandler
import com.badlogic.palermojustice.controller.MessageType
import com.badlogic.palermojustice.firebase.FirebaseInterface
import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GameState
import android.content.Context
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp

class NetworkController private constructor(private val context: Context) : FirebaseInterface {
    private val database = Firebase.database.reference
    private val messageHandler = MessageHandler()
    private var roomReference: DatabaseReference? = null
    private var playerReference: DatabaseReference? = null
    private var roomListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null

    companion object {
        @JvmStatic
        private var instance: NetworkController? = null

        @JvmStatic
        fun initialize(context: Context): NetworkController {
            // Ensure Firebase is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            instance = NetworkController(context)
            return instance!!
        }

        @JvmStatic
        fun getInstance(): NetworkController {
            return instance ?: throw IllegalStateException("NetworkController must be initialized before getting instance")
        }
    }

    override fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        try {
            // Get reference to the room
            roomReference = database.child("rooms").child(roomId)

            // Add player to the room
            val playerId = database.child("players").push().key ?: return
            playerReference = database.child("players").child(playerId)

            // Create player object
            val player = mapOf(
                "name" to playerName,
                "isAlive" to true,
                "role" to "PAESANO" // Default role, will be assigned by game logic
            )

            // Update database
            val updates = hashMapOf<String, Any>(
                "players/$playerId" to player,
                "rooms/$roomId/players/$playerId" to true
            )

            database.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Start listening for room updates
                    startRoomListener(roomId)
                    // Start listening for messages
                    startMessagesListener(roomId)
                    callback(true)
                } else {
                    callback(false)
                }
            }
        } catch (e: Exception) {
            callback(false)
        }
    }

    override fun sendMessage(messageType: String, data: Map<String, Any>) {
        try {
            val type = MessageType.valueOf(messageType)

            sendMessage(type, data)
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
        }
    }

    override fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit) {
        messageHandler.registerCallback(MessageType.GAME_STATE_UPDATE) { message ->
            try {
                val payloadMap = message.payload as? Map<String, Any> ?:
                messageHandler.json.toJson(message.payload).let {
                    messageHandler.json.fromJson(Map::class.java, it) as Map<String, Any>
                }

                updateCallback(payloadMap)
            } catch (e: Exception) {
                println("Error converting payload: ${e.message}")
            }
        }

        if (roomListener == null && roomReference != null) {
            roomReference?.key?.let { roomId ->
                startRoomListener(roomId)
            }
        }
    }

    private fun startRoomListener(roomId: String) {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Convert snapshot to GameState
                val gameState = snapshot.getValue(GameState::class.java)
                gameState?.let {
                    // Create a GAME_STATE_UPDATE message
                    val gameMessage = GameMessage(MessageType.GAME_STATE_UPDATE, it)
                    // Send message to router
                    messageHandler.routeMessage(gameMessage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
                println("Room listener cancelled: ${error.message}")
            }
        }

        roomReference?.addValueEventListener(roomListener!!)
    }

    private fun startMessagesListener(roomId: String) {
        val messagesRef = database.child("rooms").child(roomId).child("messages")

        messagesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(FirebaseMessage::class.java)
                message?.let {
                    // Convert Firebase message to GameMessage
                    val gameMessage = GameMessage(
                        type = MessageType.valueOf(it.type),
                        payload = it.data
                    )
                    messageHandler.routeMessage(gameMessage)

                    // Optional: remove messages after processing
                    snapshot.ref.removeValue()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
                println("Messages listener cancelled: ${error.message}")
            }
        }

        messagesRef.addChildEventListener(messagesListener!!)
    }

    fun sendMessage(messageType: MessageType, data: Any) {
        try {
            // Create Firebase message
            val firebaseMessage = FirebaseMessage(
                type = messageType.name,
                data = data,
                timestamp = ServerValue.TIMESTAMP
            )

            // Push message to room's messages node
            roomReference?.child("messages")?.push()?.setValue(firebaseMessage)
        } catch (e: Exception) {
            // Handle error
            println("Error creating or sending message: ${e.message}")
        }
    }

    override fun disconnect() {
        // Remove listeners
        roomListener?.let { roomReference?.removeEventListener(it) }
        messagesListener?.let { roomReference?.child("messages")?.removeEventListener(it) }

        // Remove player from room
        playerReference?.let { ref ->
            ref.removeValue()
        }
    }

    // Data class for Firebase messages
    data class FirebaseMessage(
        val type: String = "",
        val data: Any = mapOf<String, Any>(),
        val timestamp: Any? = null
    )
}
