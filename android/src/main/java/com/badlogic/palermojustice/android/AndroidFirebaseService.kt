package com.badlogic.palermojustice.firebase

import com.badlogic.palermojustice.android.NetworkController
import com.badlogic.palermojustice.controller.MessageType


class AndroidFirebaseService : FirebaseInterface {
    private val networkController = NetworkController.getInstance()

    override fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        networkController.connectToRoom(roomId, playerName, callback)
    }

    override fun sendMessage(messageType: String, data: Map<String, Any>) {
        networkController.sendMessage(MessageType.valueOf(messageType), data)
    }

    override fun listenForGameUpdates(updateCallback: (Map<String, Any>) -> Unit) {
        // Implement logic to listen for game updates
        // Could use NetworkController to configure listeners
    }

    override fun disconnect() {
        networkController.disconnect()
    }
}
