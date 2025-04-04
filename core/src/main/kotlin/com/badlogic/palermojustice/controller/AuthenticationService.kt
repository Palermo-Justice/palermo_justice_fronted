package com.badlogic.palermojustice.service

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net

class AuthenticationService {
    fun login(username: String, password: String, callback: (Boolean) -> Unit) {
        val httpRequest = Net.HttpRequest(Net.HttpMethods.POST)
        httpRequest.url = "https://your-server/api/auth/login"
        httpRequest.content = "{\"username\":\"$username\",\"password\":\"$password\"}"
        httpRequest.setHeader("Content-Type", "application/json")

        Gdx.net.sendHttpRequest(httpRequest, object : Net.HttpResponseListener {
            override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
                val responseJson = httpResponse.resultAsString
                // Estrai token e salvalo per usi futuri
                val token = parseTokenFromResponse(responseJson)
                if (token.isNotEmpty()) {
                    // Salva token
                    saveToken(token)
                    callback(true)
                } else {
                    callback(false)
                }
            }

            override fun failed(t: Throwable) {
                Gdx.app.error("AUTH", "Login failed", t)
                callback(false)
            }

            override fun cancelled() {
                callback(false)
            }
        })
    }

    fun register(userData: UserData, callback: (Boolean) -> Unit) {
        // Implementazione simile al login
    }

    fun validateToken(token: String, callback: (Boolean) -> Unit) {
        // Implementazione per validare token
    }

    private fun parseTokenFromResponse(responseJson: String): String {
        // Implementa parsing
        return ""
    }

    private fun saveToken(token: String) {
        // Salva token in preferences
        val prefs = Gdx.app.getPreferences("auth_prefs")
        prefs.putString("auth_token", token)
        prefs.flush()
    }
}

data class UserData(
    val username: String,
    val password: String,
    val email: String
)
