package com.badlogic.palermojustice.firebase

import com.badlogic.palermojustice.model.Player

interface FirebaseInterface {
    fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit)
    fun createRoom(hostName: String, roomSettings: Map<String, Any>, callback: (String?) -> Unit)
    fun sendMessage(messageType: String, data: Map<String, Any>)
    fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit)
    fun getRoomInfo(roomId: String, callback: (Map<String, Any>?) -> Unit)
    fun setPlayerDead(roomId: String, playerId: String, callback: (Boolean) -> Unit)
    fun setPlayerProtected(roomId: String, playerId: String, isProtected: Boolean, callback: (Boolean) -> Unit)
    fun getAllPlayers(roomId: String, callback: (List<Player>?) -> Unit)
    fun listenForConfirmations(callback: (List<String>) -> Unit)
    fun disconnect()
    fun updatePlayerAttribute(playerId: String, attribute: String, value: Any, callback: (Boolean) -> Unit = {})
    fun registerVote(voterId: String, targetId: String?, callback: (Boolean) -> Unit)
    fun listenForVotes(callback: (Map<String, String?>) -> Unit)
    fun resetVotes(callback: (Boolean) -> Unit)
}
