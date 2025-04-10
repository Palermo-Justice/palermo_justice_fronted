package com.badlogic.palermojustice.android

import com.badlogic.palermojustice.controller.GameMessage
import com.badlogic.palermojustice.controller.MessageHandler
import com.badlogic.palermojustice.controller.MessageType
import com.badlogic.palermojustice.firebase.FirebaseInterface
import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.GameState
import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp

class NetworkController private constructor(private val context: Context) : FirebaseInterface {
    private val TAG = "NetworkController"
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
            Log.d("NetworkController", "Initializing NetworkController")
            // Ensure Firebase is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d("NetworkController", "Initializing Firebase app")
                FirebaseApp.initializeApp(context)
            } else {
                Log.d("NetworkController", "Firebase app already initialized")
            }

            instance = NetworkController(context)
            Log.d("NetworkController", "NetworkController instance created")
            return instance!!
        }

        @JvmStatic
        fun getInstance(): NetworkController {
            return instance ?: throw IllegalStateException("NetworkController must be initialized before getting instance")
        }
    }

    override fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "connectToRoom: Attempting to connect to room $roomId as $playerName")
        try {
            // Get reference to the room
            roomReference = database.child("rooms").child(roomId)
            Log.d(TAG, "connectToRoom: Room reference created: ${roomReference?.key}")

            // Add player to the room
            val playerId = database.child("players").push().key
            if (playerId == null) {
                Log.e(TAG, "connectToRoom: Failed to generate player ID")
                callback(false)
                return
            }
            Log.d(TAG, "connectToRoom: Generated player ID: $playerId")

            playerReference = database.child("players").child(playerId)
            Log.d(TAG, "connectToRoom: Player reference created")

            // Create player object
            val player = mapOf(
                "name" to playerName,
                "isAlive" to true,
                "role" to "PAESANO" // Default role, will be assigned by game logic
            )
            Log.d(TAG, "connectToRoom: Player object created")

            // Update database
            val updates = hashMapOf<String, Any>(
                "players/$playerId" to player,
                "rooms/$roomId/players/$playerId" to true
            )
            Log.d(TAG, "connectToRoom: Preparing to update database with: $updates")

            database.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "connectToRoom: Database update successful")
                    // Start listening for room updates
                    startRoomListener(roomId)
                    // Start listening for messages
                    startMessagesListener(roomId)
                    Log.d(TAG, "connectToRoom: Calling callback with success=true")
                    callback(true)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "connectToRoom: Database update failed", exception)
                    Log.d(TAG, "connectToRoom: Calling callback with success=false")
                    callback(false)
                }
                .addOnCompleteListener { task ->
                    Log.d(TAG, "connectToRoom: Database update operation completed. Success: ${task.isSuccessful}")
                    if (task.exception != null) {
                        Log.e(TAG, "connectToRoom: Task exception", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "connectToRoom: Exception occurred", e)
            callback(false)
        }
    }

    override fun sendMessage(messageType: String, data: Map<String, Any>) {
        Log.d(TAG, "sendMessage: Attempting to send message of type $messageType")
        try {
            val type = MessageType.valueOf(messageType)
            Log.d(TAG, "sendMessage: Message type parsed successfully")
            sendMessage(type, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
        }
    }

    override fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit) {
        Log.d(TAG, "listenForGameUpdates: Setting up game update listener")
        messageHandler.registerCallback(MessageType.GAME_STATE_UPDATE) { message ->
            Log.d(TAG, "listenForGameUpdates: Received GAME_STATE_UPDATE message")
            try {
                val payloadMap = message.payload as? Map<String, Any> ?:
                messageHandler.json.toJson(message.payload).let {
                    Log.d(TAG, "listenForGameUpdates: Converting payload using JSON")
                    messageHandler.json.fromJson(Map::class.java, it) as Map<String, Any>
                }

                Log.d(TAG, "listenForGameUpdates: Invoking update callback with payload")
                updateCallback(payloadMap)
            } catch (e: Exception) {
                Log.e(TAG, "Error converting payload", e)
            }
        }

        if (roomListener == null && roomReference != null) {
            Log.d(TAG, "listenForGameUpdates: Room listener not yet set up, initializing")
            roomReference?.key?.let { roomId ->
                startRoomListener(roomId)
            }
        } else {
            Log.d(TAG, "listenForGameUpdates: Room listener already set up or room reference is null")
        }
    }

    private fun startRoomListener(roomId: String) {
        Log.d(TAG, "startRoomListener: Setting up room listener for room $roomId")
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "startRoomListener.onDataChange: Received data change for room")
                // Convert snapshot to GameState
                val gameState = snapshot.getValue(GameState::class.java)
                if (gameState != null) {
                    Log.d(TAG, "startRoomListener.onDataChange: Successfully converted to GameState")
                    // Create a GAME_STATE_UPDATE message
                    val gameMessage = GameMessage(MessageType.GAME_STATE_UPDATE, gameState)
                    // Send message to router
                    messageHandler.routeMessage(gameMessage)
                    Log.d(TAG, "startRoomListener.onDataChange: Message routed to handler")
                } else {
                    Log.w(TAG, "startRoomListener.onDataChange: Failed to convert snapshot to GameState")
                    Log.d(TAG, "startRoomListener.onDataChange: Snapshot: ${snapshot.value}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
                Log.e(TAG, "Room listener cancelled: ${error.message}", Exception(error.toException()))
            }
        }

        roomReference?.addValueEventListener(roomListener!!)
        Log.d(TAG, "startRoomListener: Room listener registered")
    }

    private fun startMessagesListener(roomId: String) {
        Log.d(TAG, "startMessagesListener: Setting up messages listener for room $roomId")
        val messagesRef = database.child("rooms").child(roomId).child("messages")
        Log.d(TAG, "startMessagesListener: Messages reference path: ${messagesRef.path}")

        messagesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "startMessagesListener.onChildAdded: New message detected")
                val message = snapshot.getValue(FirebaseMessage::class.java)
                if (message != null) {
                    Log.d(TAG, "startMessagesListener.onChildAdded: Message parsed successfully. Type: ${message.type}")
                    // Convert Firebase message to GameMessage
                    try {
                        val gameMessage = GameMessage(
                            type = MessageType.valueOf(message.type),
                            payload = message.data
                        )
                        messageHandler.routeMessage(gameMessage)
                        Log.d(TAG, "startMessagesListener.onChildAdded: Message routed to handler")

                        // Optional: remove messages after processing
                        snapshot.ref.removeValue()
                        Log.d(TAG, "startMessagesListener.onChildAdded: Message removed from database after processing")
                    } catch (e: Exception) {
                        Log.e(TAG, "startMessagesListener.onChildAdded: Error processing message", e)
                    }
                } else {
                    Log.w(TAG, "startMessagesListener.onChildAdded: Failed to parse message")
                    Log.d(TAG, "startMessagesListener.onChildAdded: Snapshot: ${snapshot.value}")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "startMessagesListener.onChildChanged: Message changed")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d(TAG, "startMessagesListener.onChildRemoved: Message removed")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "startMessagesListener.onChildMoved: Message moved")
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
                Log.e(TAG, "Messages listener cancelled: ${error.message}", Exception(error.toException()))
            }
        }

        messagesRef.addChildEventListener(messagesListener!!)
        Log.d(TAG, "startMessagesListener: Messages listener registered")
    }

    fun sendMessage(messageType: MessageType, data: Any) {
        Log.d(TAG, "sendMessage: Preparing to send message of type ${messageType.name}")
        try {
            // Create Firebase message
            val firebaseMessage = FirebaseMessage(
                type = messageType.name,
                data = data,
                timestamp = ServerValue.TIMESTAMP
            )
            Log.d(TAG, "sendMessage: Firebase message created")

            // Push message to room's messages node
            if (roomReference == null) {
                Log.e(TAG, "sendMessage: Room reference is null, cannot send message")
                return
            }

            val messageRef = roomReference?.child("messages")?.push()
            Log.d(TAG, "sendMessage: Message reference created: ${messageRef?.key}")

            messageRef?.setValue(firebaseMessage)
                ?.addOnSuccessListener {
                    Log.d(TAG, "sendMessage: Message sent successfully")
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "sendMessage: Failed to send message", e)
                }
        } catch (e: Exception) {
            // Handle error
            Log.e(TAG, "Error creating or sending message", e)
        }
    }

    override fun disconnect() {
        Log.d(TAG, "disconnect: Disconnecting from Firebase")
        // Remove listeners
        if (roomListener != null && roomReference != null) {
            roomReference?.removeEventListener(roomListener!!)
            Log.d(TAG, "disconnect: Room listener removed")
        }

        if (messagesListener != null && roomReference != null) {
            roomReference?.child("messages")?.removeEventListener(messagesListener!!)
            Log.d(TAG, "disconnect: Messages listener removed")
        }

        // Remove player from room
        playerReference?.let { ref ->
            ref.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "disconnect: Player removed from room successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "disconnect: Failed to remove player from room", e)
                }
        }

        Log.d(TAG, "disconnect: Disconnect complete")
    }

    // Data class for Firebase messages
    data class FirebaseMessage(
        val type: String = "",
        val data: Any = mapOf<String, Any>(),
        val timestamp: Any? = null
    )
}
