package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.controller.LobbyController

class JoinGameScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var roomCodeField: TextField
    private lateinit var playerNameField: TextField
    private var loadingDialog: Dialog? = null
    private var pendingTransition: (() -> Unit)? = null

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()
    }

    private fun createUI() {
        // Main table
        val mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.top().padTop(20f).padLeft(20f).padRight(20f)
        stage.addActor(mainTable)

        // Title
        val titleLabel = Label("Join Game", skin, "title")
        titleLabel.setFontScale(3f)
        mainTable.add(titleLabel).left().padBottom(30f).row()

        // Room Code field
        val roomCodeLabel = Label("Room Code", skin)
        roomCodeLabel.setFontScale(1.2f)
        mainTable.add(roomCodeLabel).left().padBottom(10f).row()

        roomCodeField = TextField("", skin, "custom")
        mainTable.add(roomCodeField).fillX().size(540f, 150f).padBottom(30f).row()

        // Player name field
        val playerNameLabel = Label("Your Name", skin)
        playerNameLabel.setFontScale(1.2f)
        mainTable.add(playerNameLabel).left().padBottom(10f).row()

        playerNameField = TextField("", skin)
        mainTable.add(playerNameField).fillX().height(50f).padBottom(50f).row()

        // Lower buttons
        val buttonsTable = Table()

        val backButton = TextButton("Back", skin)
        backButton.pad(10f)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        val joinButton = TextButton("Join", skin)
        joinButton.pad(10f)
        joinButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Retrieve UI data
                val roomCode = roomCodeField.text.trim().uppercase()
                val playerName = playerNameField.text.trim()

                if (roomCode.isBlank() || playerName.isBlank()) {
                    showErrorDialog("Please fill in all required fields")
                    return
                }

                loadingDialog = showLoadingDialog("Joining game...")

                // First check if the room exists
                Main.instance.firebaseInterface.getRoomInfo(roomCode) { roomInfo ->
                    if (roomInfo == null) {
                        Gdx.app.postRunnable {
                            loadingDialog?.hide()
                            showErrorDialog("Room not found. Please check the code and try again.")
                        }
                        return@getRoomInfo
                    }

                    // Room exists, now try to connect
                    Main.instance.firebaseInterface.connectToRoom(roomCode, playerName) { success ->
                        if (success) {
                            // Get the game name if possible
                            val gameName = if (roomInfo.containsKey("settings")) {
                                val settings = roomInfo["settings"] as? Map<*, *>
                                settings?.get("name") as? String ?: "Game Room"
                            } else {
                                "Game Room"
                            }

                            // Schedule transition for the next render cycle
                            Gdx.app.postRunnable {
                                loadingDialog?.hide()
                                // Don't transition immediately - wait for next render cycle
                                pendingTransition = {
                                    Main.instance.setScreen(LobbyScreen(
                                        roomId = roomCode,
                                        playerName = playerName,
                                        isHost = false,
                                        gameName = gameName
                                    ))
                                }
                            }
                        } else {
                            Gdx.app.postRunnable {
                                loadingDialog?.hide()
                                showErrorDialog("Failed to join room. It may be full or game already started.")
                            }
                        }
                    }
                }
            }
        })

        buttonsTable.add(backButton).width(150f).padRight(20f)
        buttonsTable.add(joinButton).width(150f)

        mainTable.add(buttonsTable).fillX()
    }

    private fun showErrorDialog(message: String): Dialog {
        val dialog = Dialog("Error", skin)
        dialog.contentTable.add(Label(message, skin)).pad(20f)
        dialog.button("OK")
        dialog.show(stage)
        return dialog
    }

    private fun showLoadingDialog(message: String): Dialog {
        val dialog = Dialog("", skin)
        dialog.contentTable.add(Label(message, skin)).pad(20f)
        dialog.show(stage)
        return dialog
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()

        // Check if we have a pending transition and execute it
        pendingTransition?.let {
            it.invoke()
            pendingTransition = null
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }
}
