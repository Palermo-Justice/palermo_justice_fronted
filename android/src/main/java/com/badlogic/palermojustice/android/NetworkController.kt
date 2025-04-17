package com.badlogic.palermojustice.android

import com.badlogic.palermojustice.controller.GameMessage
import com.badlogic.palermojustice.controller.MessageHandler
import com.badlogic.palermojustice.controller.MessageType
import com.badlogic.palermojustice.firebase.FirebaseInterface
import android.content.Context
import android.util.Log
import com.badlogic.palermojustice.model.Ispettore
import com.badlogic.palermojustice.model.Mafioso
import com.badlogic.palermojustice.model.Paesano
import com.badlogic.palermojustice.model.Player
import com.badlogic.palermojustice.model.Sgarrista
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
    private var playerId: String? = null
    private var roomListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private var confirmationsListener: ValueEventListener? = null

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

    /**
     * Creates a new game room and returns the room ID.
     * @param hostName The name of the host player
     * @param roomSettings Optional map of initial room settings (like max players, game options, etc.)
     * @param callback Callback with roomId on success, null on failure
     */
    override fun createRoom(hostName: String, roomSettings: Map<String, Any>, callback: (String?) -> Unit) {
        Log.d(TAG, "createRoom: Creating new room with host $hostName")
        try {
            // Generate a unique room ID
            val roomId = generateRoomId()
            Log.d(TAG, "createRoom: Generated room ID: $roomId")

            // Create initial room state as a Map
            val initialRoomState = mapOf(
                "roomId" to roomId,
                "state" to "WAITING",
                "hostName" to hostName, // Store host name directly
                "players" to mapOf<String, Any>(), // Empty players map initially
                "currentPhase" to "LOBBY",
                "settings" to roomSettings,
                "createdAt" to ServerValue.TIMESTAMP,
                "currentNightRoleIndex" to 0,
                "confirmations" to mapOf<String, Any>() // Add confirmations map
            )
            Log.d(TAG, "createRoom: Initial room state created")

            // Create the room in Firebase
            val roomRef = database.child("rooms").child(roomId)
            roomRef.setValue(initialRoomState)
                .addOnSuccessListener {
                    Log.d(TAG, "createRoom: Room created successfully in Firebase")

                    // Now connect the host to the room
                    connectToRoom(roomId, hostName) { success ->
                        Log.d(TAG, "createRoom: connectToRoom callback received with success=$success")
                        if (success) {
                            Log.d(TAG, "createRoom: Host connected to room successfully")

                            // Update the hostPlayerId in the room if needed
                            playerId?.let { hostPlayerId ->
                                Log.d(TAG, "createRoom: Updating hostPlayerId to $hostPlayerId")
                                roomRef.child("hostPlayerId").setValue(hostPlayerId)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "createRoom: Host ID updated in room")
                                        callback(roomId)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "createRoom: Failed to update host ID", e)
                                        // Still return the room ID as the room was created
                                        callback(roomId)
                                    }
                            } ?: run {
                                Log.w(TAG, "createRoom: playerId is null, cannot update hostPlayerId")
                                callback(roomId)
                            }
                        } else {
                            Log.e(TAG, "createRoom: Failed to connect host to room")
                            // Clean up the created room
                            roomRef.removeValue()
                                .addOnCompleteListener {
                                    Log.d(TAG, "createRoom: Room cleanup completed after failed connection")
                                    callback(null)
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "createRoom: Failed to create room", e)
                    callback(null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "createRoom: Exception occurred", e)
            callback(null)
        }
    }

    /**
     * Generates a random room ID with the option to specify length.
     * Room IDs are alphanumeric for ease of sharing.
     */
    private fun generateRoomId(length: Int = 6): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Gets information about a room by its ID
     * @param roomId The ID of the room to check
     * @param callback Callback with room data if it exists, null otherwise
     */
    override fun getRoomInfo(roomId: String, callback: (Map<String, Any>?) -> Unit) {
        Log.d(TAG, "getRoomInfo: Checking if room $roomId exists")
        try {
            database.child("rooms").child(roomId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "getRoomInfo: Room exists")
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val roomData = snapshot.getValue() as? Map<String, Any>
                            // Ensure the room has a confirmations node
                            if (roomData != null && !roomData.containsKey("confirmations")) {
                                // Add confirmations node if missing
                                val updatedData = roomData.toMutableMap()
                                updatedData["confirmations"] = mapOf<String, Any>()
                                callback(updatedData)

                                // Update the room with confirmations node
                                snapshot.ref.child("confirmations").setValue(mapOf<String, Any>())
                                    .addOnSuccessListener {
                                        Log.d(TAG, "getRoomInfo: Added confirmations node to room")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "getRoomInfo: Failed to add confirmations node", e)
                                    }
                            } else {
                                callback(roomData)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "getRoomInfo: Error casting data", e)
                            callback(null)
                        }
                    } else {
                        Log.d(TAG, "getRoomInfo: Room does not exist")
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "getRoomInfo: Error checking room", error.toException())
                    callback(null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "getRoomInfo: Exception occurred", e)
            callback(null)
        }
    }

    override fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "connectToRoom: Attempting to connect to room $roomId as $playerName")
        try {
            // Get reference to the room
            roomReference = database.child("rooms").child(roomId)
            Log.d(TAG, "connectToRoom: Room reference created: ${roomReference?.key}")

            // Generate player ID
            playerId = database.push().key
            if (playerId == null) {
                Log.e(TAG, "connectToRoom: Failed to generate player ID")
                callback(false)
                return
            }
            Log.d(TAG, "connectToRoom: Generated player ID: $playerId")

            // Set up player reference within the room
            playerReference = roomReference?.child("players")?.child(playerId!!)
            Log.d(TAG, "connectToRoom: Player reference created")

            // Create player object
            val player = mapOf(
                "name" to playerName,
                "isAlive" to true,
                "joinedAt" to ServerValue.TIMESTAMP,
                "confirmed" to false, // Make sure it's a boolean false, not a string
                "protected" to false // Make sure it's a boolean false, not a string
            )
            Log.d(TAG, "connectToRoom: Player object created")

            // Add player directly to the room's players node
            playerReference?.setValue(player)
                ?.addOnSuccessListener {
                    Log.d(TAG, "connectToRoom: Player added to room successfully")

                    // Ensure confirmations node exists
                    roomReference?.child("confirmations")?.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                // Create confirmations node if it doesn't exist
                                roomReference?.child("confirmations")?.setValue(mapOf<String, Any>())
                                    ?.addOnSuccessListener {
                                        Log.d(TAG, "connectToRoom: Created confirmations node")
                                    }
                                    ?.addOnFailureListener { e ->
                                        Log.e(TAG, "connectToRoom: Failed to create confirmations node", e)
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "connectToRoom: Error checking confirmations node", error.toException())
                        }
                    })

                    // Start listening for room updates
                    startRoomListener(roomId)
                    // Start listening for messages
                    startMessagesListener(roomId)
                    Log.d(TAG, "connectToRoom: Calling callback with success=true")
                    callback(true)
                }
                ?.addOnFailureListener { exception ->
                    Log.e(TAG, "connectToRoom: Failed to add player to room", exception)
                    Log.d(TAG, "connectToRoom: Calling callback with success=false")
                    callback(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "connectToRoom: Exception occurred", e)
            callback(false)
        }
    }

    /**
     * Sets the 'isAlive' status of a player to false in the specified room.
     *
     * @param roomId The ID of the room containing the player.
     * @param playerId The ID of the player whose 'isAlive' status needs to be updated.
     * @param callback Callback indicating success or failure of the operation.
     */
    override fun setPlayerDead(roomId: String, playerId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "setPlayerDead: Setting player $playerId in room $roomId to dead")

        try {
            val playerRef = database.child("rooms").child(roomId).child("players").child(playerId).child("isAlive")

            playerRef.setValue(false)
                .addOnSuccessListener {
                    Log.d(TAG, "setPlayerDead: Player $playerId set to dead successfully")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "setPlayerDead: Failed to set player $playerId to dead", e)
                    callback(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "setPlayerDead: Exception occurred while setting player dead", e)
            callback(false)
        }
    }

    /**
     * Sets the 'protected' status of a player in the specified room.
     *
     * @param roomId The ID of the room containing the player.
     * @param playerId The ID of the player whose 'protected' status needs to be updated.
     * @param isProtected The boolean value to set for the 'protected' status (true or false).
     * @param callback Callback indicating success or failure of the operation.
     */
    override fun setPlayerProtected(roomId: String, playerId: String, isProtected: Boolean, callback: (Boolean) -> Unit) {
        Log.d(TAG, "setPlayerProtected: Setting player $playerId in room $roomId to protected = $isProtected")

        try {
            val playerRef = database.child("rooms").child(roomId).child("players").child(playerId).child("protected")

            playerRef.setValue(isProtected)
                .addOnSuccessListener {
                    Log.d(TAG, "setPlayerProtected: Player $playerId set to protected = $isProtected successfully")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "setPlayerProtected: Failed to set player $playerId to protected = $isProtected", e)
                    callback(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "setPlayerProtected: Exception occurred while setting player protected", e)
            callback(false)
        }
    }

    /**
     * Retrieves all players currently in the specified room.
     *
     * @param roomId The ID of the room.
     * @param callback Callback that will receive a list of Player objects if successful,
     * or null if there's an error or no players are found.
     */
    override fun getAllPlayers(roomId: String, callback: (List<Player>?) -> Unit) {
        Log.d(TAG, "getAllPlayers: Fetching all players in room $roomId")

        database.child("rooms").child(roomId).child("players")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val playerList = mutableListOf<Player>()
                    if (snapshot.exists()) {
                        for (playerSnapshot in snapshot.children) {
                            try {
                                val playerId = playerSnapshot.key
                                val playerData = playerSnapshot.getValue() as? Map<String, Any>
                                if (playerId != null && playerData != null) {
                                    val player = Player()
                                    player.id = playerId
                                    player.name = playerData["name"] as? String ?: ""
                                    player.isAlive = playerData["isAlive"] as? Boolean ?: true
                                    player.isProtected = playerData["protected"] as? Boolean ?: false // Assuming 'protected' exists
                                    val roleName = playerData["role"] as? String
                                    player.role = when (roleName) {
                                        "Mafioso" -> Mafioso() // Assuming these role classes exist
                                        "Ispettore" -> Ispettore()
                                        "Sgarrista" -> Sgarrista()
                                        "Paesano" -> Paesano()
                                        else -> Paesano() // Default role if not recognized
                                    }
                                    playerList.add(player)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "getAllPlayers: Error parsing player data", e)
                                callback(null)
                                return
                            }
                        }
                        Log.d(TAG, "getAllPlayers: Successfully retrieved ${playerList.size} players")
                        callback(playerList)
                    } else {
                        Log.d(TAG, "getAllPlayers: No players found in room $roomId")
                        callback(emptyList()) // Return an empty list if no players
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "getAllPlayers: Database error while fetching players", error.toException())
                    callback(null)
                }
            })
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
                @Suppress("UNCHECKED_CAST")
                val payloadMap = message.payload as? Map<String, Any> ?:
                messageHandler.json.toJson(message.payload).let {
                    Log.d(TAG, "listenForGameUpdates: Converting payload using JSON")
                    @Suppress("UNCHECKED_CAST")
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
                try {
                    // Get all data as Map
                    @Suppress("UNCHECKED_CAST")
                    val roomData = snapshot.getValue() as? Map<String, Any>

                    if (roomData != null) {
                        Log.d(TAG, "startRoomListener.onDataChange: Room data parsed")

                        // Create a GameMessage with complete room data
                        val gameMessage = GameMessage(
                            type = MessageType.GAME_STATE_UPDATE,
                            payload = roomData
                        )

                        // Send the message to the message handler
                        Log.d(TAG, "startRoomListener.onDataChange: About to route message")
                        messageHandler.routeMessage(gameMessage)
                        Log.d(TAG, "startRoomListener.onDataChange: Message routed to handler")
                    } else {
                        Log.w(TAG, "startRoomListener.onDataChange: Room data is null")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "startRoomListener.onDataChange: Error processing room data", e)
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

        messagesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "startMessagesListener.onChildAdded: New message detected")
                try {
                    @Suppress("UNCHECKED_CAST")
                    val message = snapshot.getValue(FirebaseMessage::class.java)
                    if (message != null) {
                        Log.d(TAG, "startMessagesListener.onChildAdded: Message parsed successfully. Type: ${message.type}")
                        // Convert Firebase message to GameMessage
                        try {
                            // Add this section to specifically handle START_GAME messages
                            if (message.type == "START_GAME") {
                                Log.d(TAG, "startMessagesListener.onChildAdded: Received START_GAME message")

                                // Directly update the room state to RUNNING
                                roomReference?.child("state")?.setValue("RUNNING")
                                    ?.addOnSuccessListener {
                                        Log.d(TAG, "startMessagesListener.onChildAdded: Room state updated to RUNNING")
                                    }
                                    ?.addOnFailureListener { e ->
                                        Log.e(TAG, "startMessagesListener.onChildAdded: Failed to update room state", e)
                                    }
                            }

                            // Handle confirmation messages specially
                            if (message.type == "CONFIRMATION") {
                                Log.d(TAG, "startMessagesListener.onChildAdded: Received CONFIRMATION message")

                                // Extract data from the message
                                @Suppress("UNCHECKED_CAST")
                                val data = message.data as? Map<String, Any>
                                val playerId = data?.get("playerId") as? String
                                val rolePhase = data?.get("rolePhase") as? String
                                val confirmed = data?.get("confirmed") as? Boolean ?: true

                                if (playerId != null) {
                                    // Update the player's confirmed status
                                    roomReference?.child("players")?.child(playerId)?.child("confirmed")?.setValue(confirmed)

                                    // Also update the confirmations node
                                    roomReference?.child("confirmations")?.child(playerId)?.setValue(confirmed)

                                    Log.d(TAG, "startMessagesListener.onChildAdded: Updated confirmation status for player $playerId")
                                }
                            }

                            // Continue with normal message handling
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
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "startMessagesListener.onChildAdded: Error parsing message", e)
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

        if (confirmationsListener != null && roomReference != null) {
            roomReference?.child("confirmations")?.removeEventListener(confirmationsListener!!)
            Log.d(TAG, "disconnect: Confirmations listener removed")
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

    override fun updatePlayerAttribute(playerId: String, attribute: String, value: Any, callback: (Boolean) -> Unit) {
        Log.d(TAG, "updatePlayerAttribute: Aggiornamento diretto di $attribute a $value per player $playerId")

        if (roomReference == null) {
            Log.e(TAG, "updatePlayerAttribute: Nessun riferimento alla stanza")
            callback(false)
            return
        }

        roomReference?.child("players")?.child(playerId)?.child(attribute)?.setValue(value)
            ?.addOnSuccessListener {
                Log.d(TAG, "updatePlayerAttribute: Aggiornamento riuscito")
                callback(true)
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "updatePlayerAttribute: Errore nell'aggiornamento", e)
                callback(false)
            }
    }

    override fun listenForConfirmations(callback: (List<String>) -> Unit) {

        roomReference?.child("players")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val confirmedPlayerIds = mutableListOf<String>()

                for (playerSnapshot in snapshot.children) {
                    val playerId = playerSnapshot.key ?: continue
                    val playerData = playerSnapshot.getValue() as? Map<*, *> ?: continue

                    val isAlive = playerData["isAlive"] as? Boolean ?: true
                    val isConfirmed = playerData["confirmed"] as? Boolean ?: false

                    if (isAlive && isConfirmed) {
                        confirmedPlayerIds.add(playerId)
                    }
                }

                callback(confirmedPlayerIds)
            }

            override fun onCancelled(error: DatabaseError) {
                // Log error
                callback(emptyList())
            }
        })
    }

    // Data class for Firebase messages
    data class FirebaseMessage(
        val type: String = "",
        val data: Any = mapOf<String, Any>(),
        val timestamp: Any? = null
    )
}
