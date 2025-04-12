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
                val roomCode = roomCodeField.text
                val playerName = playerNameField.text

                if (roomCode.isBlank() || playerName.isBlank()) {
                    showErrorDialog("Please fill in all required fields")
                    return
                }

                val loadingDialog = showLoadingDialog("Joining game...")

                // Create a controller just for joining
                val controller = LobbyController(
                    Main.instance.firebaseInterface,
                    roomCode,
                    playerName,
                    false
                )

                controller.joinRoom(roomCode, playerName) { success ->
                    Gdx.app.postRunnable {
                        loadingDialog.hide()

                        if (success) {
                            Main.instance.setScreen(LobbyScreen(
                                roomId = roomCode,
                                playerName = playerName,
                                isHost = false
                            ))
                        } else {
                            showErrorDialog("Failed to join game. Please check the room code and try again.")
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
