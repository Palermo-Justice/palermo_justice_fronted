package com.badlogic.palermojustice.controller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.net.Socket
import com.badlogic.gdx.net.SocketHints
import java.io.IOException

class NetworkController private constructor() {
    private var socket: Socket? = null
    private val messageHandler = MessageHandler()

    companion object {
        private var instance: NetworkController? = null

        fun getInstance(): NetworkController {
            if (instance == null) {
                instance = NetworkController()
            }
            return instance!!
        }
    }

    fun connectToRoom(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        // Implementa la connessione socket al server
        try {
            val socketHints = SocketHints()
            socket = Gdx.net.newClientSocket(
                Net.Protocol.TCP,  // Usa Net.Protocol invece di Socket.Protocol
                "your-server-address",
                8080, // porta del server
                socketHints
            )

            // Invia messaggio di join
            val joinMessage = messageHandler.encodeMessage(
                MessageType.JOIN_ROOM,
                mapOf("roomId" to roomId, "playerName" to playerName)
            )
            sendMessage(joinMessage)

            // Avvia thread per ascoltare messaggi
            startListenerThread()

            callback(true)
        } catch (e: Exception) {
            Gdx.app.error("NETWORK", "Failed to connect to room", e)
            callback(false)
        }
    }

    fun sendMessage(bytes: ByteArray) {
        try {
            socket?.outputStream?.write(bytes)
        } catch (e: IOException) {
            Gdx.app.error("NETWORK", "Failed to send message", e)
        }
    }

    fun handleMessage(bytes: ByteArray) {
        val gameMessage = messageHandler.decodeMessage(bytes)
        messageHandler.routeMessage(gameMessage)
    }

    private fun startListenerThread() {
        Thread {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val bytesRead = socket?.inputStream?.read(buffer)
                    if (bytesRead != null && bytesRead > 0) {
                        val message = buffer.copyOfRange(0, bytesRead)
                        handleMessage(message)
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("NETWORK", "Error in listener thread", e)
            }
        }.start()
    }

    fun disconnect() {
        socket?.dispose()
    }
}
