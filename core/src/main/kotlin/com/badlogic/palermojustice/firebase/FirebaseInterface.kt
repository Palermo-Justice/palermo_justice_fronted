package com.badlogic.palermojustice.firebase

interface FirebaseInterface {
    fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit)
    fun sendMessage(messageType: String, data: Map<String, Any>)
    fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit)
    fun disconnect()
}
