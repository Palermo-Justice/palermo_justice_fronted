package com.badlogic.palermojustice.android

import com.badlogic.palermojustice.controller.GameMessage
import com.badlogic.palermojustice.controller.MessageHandler
import com.badlogic.palermojustice.controller.MessageType
import com.badlogic.palermojustice.firebase.FirebaseInterface
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
    private var playerId: String? = null
    private var roomListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private var confirmationsListener: ValueEventListener? = null
    private var votesListener: ValueEventListener? = null

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
            return instance
                ?: throw IllegalStateException("NetworkController must be initialized before getting instance")
        }
    }

    /**
     * Creates a new game room and returns the room ID.
     * @param hostName The name of the host player
     * @param roomSettings Optional map of initial room settings (like max players, game options, etc.)
     * @param callback Callback with roomId on success, null on failure
     */
    override fun createRoom(
        hostName: String,
        roomSettings: Map<String, Any>,
        callback: (String?) -> Unit
    ) {
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
                "confirmations" to mapOf<String, Any>(), // Add confirmations map
                "votes" to mapOf<String, Any>() // Add votes map
            )
            Log.d(TAG, "createRoom: Initial room state created")

            // Create the room in Firebase
            val roomRef = database.child("rooms").child(roomId)
            roomRef.setValue(initialRoomState)
                .addOnSuccessListener {
                    Log.d(TAG, "createRoom: Room created successfully in Firebase")

                    // Now connect the host to the room
                    connectToRoom(roomId, hostName) { success ->
                        Log.d(
                            TAG,
                            "createRoom: connectToRoom callback received with success=$success"
                        )
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
                                Log.w(
                                    TAG,
                                    "createRoom: playerId is null, cannot update hostPlayerId"
                                )
                                callback(roomId)
                            }
                        } else {
                            Log.e(TAG, "createRoom: Failed to connect host to room")
                            // Clean up the created room
                            roomRef.removeValue()
                                .addOnCompleteListener {
                                    Log.d(
                                        TAG,
                                        "createRoom: Room cleanup completed after failed connection"
                                    )
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
            database.child("rooms").child(roomId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.d(TAG, "getRoomInfo: Room exists")
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val roomData = snapshot.getValue() as? Map<String, Any>
                                // Ensure the room has required nodes
                                if (roomData != null) {
                                    val updatedData = roomData.toMutableMap()

                                    // Add confirmations node if missing
                                    if (!roomData.containsKey("confirmations")) {
                                        updatedData["confirmations"] = mapOf<String, Any>()
                                        snapshot.ref.child("confirmations")
                                            .setValue(mapOf<String, Any>())
                                            .addOnSuccessListener {
                                                Log.d(
                                                    TAG,
                                                    "getRoomInfo: Added confirmations node to room"
                                                )
                                            }
                                    }

                                    // Add votes node if missing
                                    if (!roomData.containsKey("votes")) {
                                        updatedData["votes"] = mapOf<String, Any>()
                                        snapshot.ref.child("votes").setValue(mapOf<String, Any>())
                                            .addOnSuccessListener {
                                                Log.d(TAG, "getRoomInfo: Added votes node to room")
                                            }
                                    }

                                    callback(updatedData)
                                } else {
                                    callback(null)
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
                "confirmed" to false // Make sure it's a boolean false, not a string
            )
            Log.d(TAG, "connectToRoom: Player object created")

            // Add player directly to the room's players node
            playerReference?.setValue(player)
                ?.addOnSuccessListener {
                    Log.d(TAG, "connectToRoom: Player added to room successfully")

                    // Ensure required nodes exist
                    ensureRoomNodesExist(roomId) {
                        // Start listeners
                        startRoomListener(roomId)
                        startMessagesListener(roomId)
                        startVotesListener(roomId)

                        Log.d(TAG, "connectToRoom: Calling callback with success=true")
                        callback(true)
                    }
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
     * Ensures that all required nodes exist in the room
     */
    private fun ensureRoomNodesExist(roomId: String, callback: () -> Unit) {
        val requiredNodes = listOf("confirmations", "votes")
        var nodesChecked = 0

        for (node in requiredNodes) {
            roomReference?.child(node)?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Create node if it doesn't exist
                        roomReference?.child(node)?.setValue(mapOf<String, Any>())
                            ?.addOnSuccessListener {
                                Log.d(TAG, "ensureRoomNodesExist: Created $node node")
                                checkComplete()
                            }
                            ?.addOnFailureListener { e ->
                                Log.e(TAG, "ensureRoomNodesExist: Failed to create $node node", e)
                                checkComplete()
                            }
                    } else {
                        checkComplete()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        TAG,
                        "ensureRoomNodesExist: Error checking $node node",
                        error.toException()
                    )
                    checkComplete()
                }

                private fun checkComplete() {
                    nodesChecked++
                    if (nodesChecked == requiredNodes.size) {
                        callback()
                    }
                }
            })
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
            val playerRef = database.child("rooms").child(roomId).child("players").child(playerId)
                .child("isAlive")

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
                val payloadMap = message.payload as? Map<String, Any> ?: messageHandler.json.toJson(
                    message.payload
                ).let {
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
            Log.d(
                TAG,
                "listenForGameUpdates: Room listener already set up or room reference is null"
            )
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
                Log.e(
                    TAG,
                    "Room listener cancelled: ${error.message}",
                    Exception(error.toException())
                )
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
                        Log.d(
                            TAG,
                            "startMessagesListener.onChildAdded: Message parsed successfully. Type: ${message.type}"
                        )
                        // Convert Firebase message to GameMessage
                        try {
                            // Add this section to specifically handle START_GAME messages
                            if (message.type == "START_GAME") {
                                Log.d(
                                    TAG,
                                    "startMessagesListener.onChildAdded: Received START_GAME message"
                                )

                                // Directly update the room state to RUNNING
                                roomReference?.child("state")?.setValue("RUNNING")
                                    ?.addOnSuccessListener {
                                        Log.d(
                                            TAG,
                                            "startMessagesListener.onChildAdded: Room state updated to RUNNING"
                                        )
                                    }
                                    ?.addOnFailureListener { e ->
                                        Log.e(
                                            TAG,
                                            "startMessagesListener.onChildAdded: Failed to update room state",
                                            e
                                        )
                                    }
                            }

                            // Handle vote messages separately
                            if (message.type == "VOTE") {
                                Log.d(
                                    TAG,
                                    "startMessagesListener.onChildAdded: Received VOTE message"
                                )

                                // Extract data from the message
                                @Suppress("UNCHECKED_CAST")
                                val data = message.data as? Map<String, Any>
                                val voterId = data?.get("voterId") as? String
                                val targetId = data?.get("targetId") as? String
                                val voteType = data?.get("voteType") as? String ?: "default"

                                if (voterId != null && targetId != null) {
                                    // Update the votes node
                                    roomReference?.child("votes")?.child(voteType)?.child(voterId)
                                        ?.setValue(targetId)
                                        ?.addOnSuccessListener {
                                            Log.d(
                                                TAG,
                                                "startMessagesListener.onChildAdded: Vote recorded for player $voterId targeting $targetId"
                                            )
                                        }
                                }
                            }

                            // Handle confirmation messages specially
                            if (message.type == "CONFIRMATION") {
                                Log.d(
                                    TAG,
                                    "startMessagesListener.onChildAdded: Received CONFIRMATION message"
                                )

                                // Extract data from the message
                                @Suppress("UNCHECKED_CAST")
                                val data = message.data as? Map<String, Any>
                                val playerId = data?.get("playerId") as? String
                                val rolePhase = data?.get("rolePhase") as? String
                                val confirmed = data?.get("confirmed") as? Boolean ?: true

                                if (playerId != null) {
                                    // Update the player's confirmed status
                                    roomReference?.child("players")?.child(playerId)
                                        ?.child("confirmed")?.setValue(confirmed)

                                    // Also update the confirmations node
                                    roomReference?.child("confirmations")?.child(playerId)
                                        ?.setValue(confirmed)

                                    Log.d(
                                        TAG,
                                        "startMessagesListener.onChildAdded: Updated confirmation status for player $playerId"
                                    )
                                }
                            }

                            // Continue with normal message handling
                            val gameMessage = GameMessage(
                                type = MessageType.valueOf(message.type),
                                payload = message.data
                            )
                            messageHandler.routeMessage(gameMessage)
                            Log.d(
                                TAG,
                                "startMessagesListener.onChildAdded: Message routed to handler"
                            )

                            // Optional: remove messages after processing
                            snapshot.ref.removeValue()
                            Log.d(
                                TAG,
                                "startMessagesListener.onChildAdded: Message removed from database after processing"
                            )
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "startMessagesListener.onChildAdded: Error processing message",
                                e
                            )
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
                Log.e(
                    TAG,
                    "Messages listener cancelled: ${error.message}",
                    Exception(error.toException())
                )
            }
        }

        messagesRef.addChildEventListener(messagesListener!!)
        Log.d(TAG, "startMessagesListener: Messages listener registered")
    }

    /**
     * Starts listening for vote changes in the room
     */
    private fun startVotesListener(roomId: String) {
        Log.d(TAG, "startVotesListener: Setting up votes listener for room $roomId")

        votesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "startVotesListener.onDataChange: Votes changed")

                try {
                    @Suppress("UNCHECKED_CAST")
                    val votesData = snapshot.getValue() as? Map<String, Any>

                    if (votesData != null) {
                        // Create a new message with the vote data
                        val gameMessage = GameMessage(
                            type = MessageType.VOTE,
                            payload = votesData
                        )

                        // Route the message to the handler
                        messageHandler.routeMessage(gameMessage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "startVotesListener.onDataChange: Error processing votes", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    TAG,
                    "Votes listener cancelled: ${error.message}",
                    Exception(error.toException())
                )
            }
        }

        roomReference?.child("votes")?.addValueEventListener(votesListener!!)
        Log.d(TAG, "startVotesListener: Votes listener registered")
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

    /**
     * Sends a vote for a player. The vote is identified by the voter's ID, the target player's ID,
     * and optionally the vote type (for different kinds of votes).
     *
     * @param targetPlayerId The ID of the player being voted for
     * @param voteType Optional type of vote (default, daytime, werewolf, etc.)
     * @param callback Callback with success status
     */
    override fun sendVote(targetPlayerId: String, voteType: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "sendVote: Player $playerId is voting for $targetPlayerId (type: $voteType)")

        if (playerId == null) {
            Log.e(TAG, "sendVote: Cannot vote without a player ID")
            callback(false)
            return
        }

        try {
            // Create vote data
            val voteData = mapOf(
                "voterId" to playerId!!,
                "targetId" to targetPlayerId,
                "voteType" to voteType,
                "timestamp" to ServerValue.TIMESTAMP
            )

            // Send using message system for immediate processing
            sendMessage(MessageType.VOTE, voteData)

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "sendVote: Error sending vote", e)
            callback(false)
        }
    }

    /**
     * Gets the current vote tally for a specific vote type
     *
     * @param voteType The type of vote to tally
     * @param callback Callback with the vote results map (targetId -> count)
     */
    override fun getVoteTally(voteType: String, callback: (Map<String, Int>) -> Unit) {
        Log.d(TAG, "getVoteTally: Getting vote tally for $voteType")

        roomReference?.child("votes")?.child(voteType)
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Count votes for each target
                        val voteTally = mutableMapOf<String, Int>()

                        for (voteSnapshot in snapshot.children) {
                            val targetId = voteSnapshot.getValue(String::class.java) ?: continue
                            voteTally[targetId] = (voteTally[targetId] ?: 0) + 1
                        }

                        Log.d(TAG, "getVoteTally: Tallied ${voteTally.size} targets with votes")
                        callback(voteTally)
                    } catch (e: Exception) {
                        Log.e(TAG, "getVoteTally: Error tallying votes", e)
                        callback(emptyMap())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "getVoteTally: Database error", error.toException())
                    callback(emptyMap())
                }
            })
    }

    /**
     * Clears all votes of a specific type
     *
     * @param voteType The type of votes to clear
     * @param callback Callback with success status
     */
    override fun clearVotes(voteType: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "clearVotes: Clearing votes of type $voteType")

        roomReference?.child("votes")?.child(voteType)?.setValue(null)
            ?.addOnSuccessListener {
                Log.d(TAG, "clearVotes: Successfully cleared votes")
                callback(true)
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "clearVotes: Failed to clear votes", e)
                callback(false)
            }
    }

    /**
     * Registers a callback for vote updates
     *
     * @param voteType The type of vote to listen for
     * @param callback Callback that will be called with the updated votes map
     */
    override fun listenForVotes(voteType: String, callback: (Map<String, String>) -> Unit) {
        Log.d(TAG, "listenForVotes: Setting up listener for vote type $voteType")

        messageHandler.registerCallback(MessageType.VOTE) { message ->
            try {
                @Suppress("UNCHECKED_CAST")
                val votesData = message.payload as? Map<String, Any> ?: return@registerCallback

                // Check if this vote type exists in the data
                if (votesData.containsKey(voteType)) {
                    val typeVotes =
                        votesData[voteType] as? Map<String, String> ?: return@registerCallback
                    callback(typeVotes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "listenForVotes: Error processing vote update", e)
            }
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

        if (votesListener != null && roomReference != null) {
            roomReference?.child("votes")?.removeEventListener(votesListener!!)
            Log.d(TAG, "disconnect: Votes listener removed")
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

    override fun updatePlayerAttribute(
        playerId: String,
        attribute: String,
        value: Any,
        callback: (Boolean) -> Unit
    ) {
        Log.d(TAG, "updatePlayerAttribute: Updating $attribute to $value for player $playerId")

        if (roomReference == null) {
            Log.e(TAG, "updatePlayerAttribute: No room reference")
            callback(false)
            return
        }

        roomReference?.child("players")?.child(playerId)?.child(attribute)?.setValue(value)
            ?.addOnSuccessListener {
                Log.d(TAG, "updatePlayerAttribute: Update successful")
                callback(true)
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "updatePlayerAttribute: Error during update", e)
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
