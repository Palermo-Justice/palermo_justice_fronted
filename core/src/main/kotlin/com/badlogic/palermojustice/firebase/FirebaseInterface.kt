package com.badlogic.palermojustice.firebase

interface FirebaseInterface {
    fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit)
    fun createRoom(hostName: String, roomSettings: Map<String, Any>, callback: (String?) -> Unit)
    fun sendMessage(messageType: String, data: Map<String, Any>)
    fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit)
    fun getRoomInfo(roomId: String, callback: (Map<String, Any>?) -> Unit)
    fun disconnect()

    // Player management
    fun updatePlayerAttribute(playerId: String, attribute: String, value: Any, callback: (Boolean) -> Unit)
    fun setPlayerDead(roomId: String, playerId: String, callback: (Boolean) -> Unit)
    fun listenForConfirmations(callback: (List<String>) -> Unit)

    // Voting system
    fun sendVote(targetPlayerId: String, voteType: String = "default", callback: (Boolean) -> Unit)
    fun getVoteTally(voteType: String = "default", callback: (Map<String, Int>) -> Unit)
    fun clearVotes(voteType: String = "default", callback: (Boolean) -> Unit)
    fun listenForVotes(voteType: String = "default", callback: (Map<String, String>) -> Unit)
}
