package com.badlogic.palermojustice.firebase

interface FirebaseInterface {
    fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit)
    fun createRoom(hostName: String, roomSettings: Map<String, Any>, callback: (String?) -> Unit)
    fun sendMessage(messageType: String, data: Map<String, Any>)
    fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit)
    fun getRoomInfo(roomId: String, callback: (Map<String, Any>?) -> Unit)
    fun setPlayerDead(roomId: String, playerId: String, callback: (Boolean) -> Unit)
    fun listenForConfirmations(callback: (List<String>) -> Unit)
    fun disconnect()
}
