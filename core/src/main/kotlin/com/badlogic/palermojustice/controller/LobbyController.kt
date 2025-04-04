package com.badlogic.palermojustice.controller
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.utils.Json


class LobbyController {
    private val networkController = NetworkController.getInstance()
    private val messageHandler = MessageHandler()

    fun createGame(roomName: String, callback: (String) -> Unit) {
        // Implementa la chiamata HTTP per creare una nuova stanza
        val httpRequest = Net.HttpRequest(Net.HttpMethods.POST)
        httpRequest.url = "https://your-server/api/rooms/create"
        httpRequest.content = "{\"roomName\":\"$roomName\"}"
        httpRequest.setHeader("Content-Type", "application/json")

        Gdx.net.sendHttpRequest(httpRequest, object : Net.HttpResponseListener {
            override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
                val responseJson = httpResponse.resultAsString
                // Parse the JSON to get the room ID
                val roomId = parseRoomIdFromResponse(responseJson)
                callback(roomId)
            }

            override fun failed(t: Throwable) {
                Gdx.app.error("NETWORK", "Failed to create room", t)
                callback("")
            }

            override fun cancelled() {
                callback("")
            }
        })
    }

    fun joinGame(roomId: String, playerName: String, callback: (Boolean) -> Unit) {
        networkController.connectToRoom(roomId, playerName, callback)
    }

    private fun parseRoomIdFromResponse(responseJson: String): String {
        // Implementa il parsing del JSON
        val json = Json()
        val response = json.fromJson(Map::class.java, responseJson) as Map<String, Any>
        return response["roomId"] as String
    }
}
