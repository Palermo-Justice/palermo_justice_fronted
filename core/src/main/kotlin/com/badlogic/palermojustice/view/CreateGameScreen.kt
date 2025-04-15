package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.firebase.FirebaseInterface


class CreateGameScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var gameNameField: TextField
    private lateinit var playerNameField: TextField
    private lateinit var playerCountSelectBox: SelectBox<String>

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
        val titleLabel = Label("Create Game", skin, "title")
        titleLabel.setFontScale(3f)
        mainTable.add(titleLabel).left().padBottom(30f).row()

        // Game name field
        val gameNameLabel = Label("Game name", skin)
        gameNameLabel.setFontScale(5f)
        mainTable.add(gameNameLabel).left().padBottom(10f).row()

        gameNameField = TextField("", skin, "custom")
        mainTable.add(gameNameField).fillX().height(100f).padBottom(50f).row()

        // Select players number
        val playerCountLabel = Label("Choose number of players", skin)
        playerCountLabel.setFontScale(5f)
        mainTable.add(playerCountLabel).left().padBottom(10f).row()

        playerCountSelectBox = SelectBox<String>(skin, "big")
        playerCountSelectBox.setItems("3", "4", "5", "6", "7")
        playerCountSelectBox.selected = "5"
        mainTable.add(playerCountSelectBox).width(200f).height(80f).padBottom(50f).row()

        // Player name field
        val playerNameLabel = Label("My player name", skin)
        playerNameLabel.setFontScale(5f)
        mainTable.add(playerNameLabel).left().padBottom(10f).row()

        playerNameField = TextField("", skin, "custom")
        mainTable.add(playerNameField).fillX().height(100f).padBottom(50f).row()

        // Lower buttons
        val buttonsTable = Table()

        val backButton = TextButton("Back", skin)
        backButton.pad(10f)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        val createButton = TextButton("Create", skin)
        createButton.pad(10f)
        createButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Retrieve UI data
                val gameName = gameNameField.text
                val playerCount = playerCountSelectBox.selected.toInt()
                val playerName = playerNameField.text

                if (gameName.isBlank() || playerName.isBlank()) {
                    showErrorDialog("Please fill in all required fields")
                    return
                }

                val roomSettings = mapOf(
                    "name" to gameName,
                    "maxPlayers" to playerCount,
                    "createdBy" to playerName
                )

                val loadingDialog = showLoadingDialog("Creating game...")

                val debugButton = TextButton("DEBUG: Force Continue", skin)
                debugButton.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        loadingDialog.hide()
                        val debugRoomId = "DEBUG_" + System.currentTimeMillis()
                        Main.instance.setScreen(LobbyScreen(debugRoomId, playerName, true, gameName))
                    }
                })
                // Set button padding before adding it to the dialog
                debugButton.pad(15f)
                debugButton.getPrefWidth()
                debugButton.getPrefHeight()
                loadingDialog.button(debugButton)

                Main.instance.firebaseInterface.createRoom(playerName, roomSettings) { roomId ->
                    Gdx.app.postRunnable {
                        loadingDialog.hide()

                        if (roomId != null) {
                            Main.instance.setScreen(LobbyScreen(
                                roomId = roomId,
                                playerName = playerName,
                                isHost = true,
                                gameName = gameName
                            ))
                        } else {
                            showErrorDialog("Failed to create game. Please try again.")
                        }
                    }
                }
            }
        })

        buttonsTable.add(backButton).size(450f, 150f).padRight(20f)
        buttonsTable.add(createButton).size(450f, 150f)

        mainTable.add(buttonsTable).fillX()
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

    /**
     * Shows an error dialog with large, readable text and improved size
     *
     * @param message The error message to display
     * @return The dialog instance that was created
     */
    private fun showErrorDialog(message: String): Dialog {
        // Create a Dialog with custom title
        val dialog = Dialog("", skin)

        // Create a larger custom title
        val titleLabel = Label("ERROR", skin, "title")
        titleLabel.setFontScale(2f)
        titleLabel.setAlignment(Align.center)
        dialog.getTitleTable().add(titleLabel).padTop(20f).padBottom(20f)

        // Create a label with enlarged text for the message
        val messageLabel = Label(message, skin, "big")
        messageLabel.setFontScale(2f)
        messageLabel.setAlignment(Align.center)
        messageLabel.setWrap(true)

        // Add the label to a container with fixed width for proper text wrapping
        dialog.contentTable.add(messageLabel).width(600f).pad(40f).row()

        // Set minimum dimensions for the dialog
        dialog.setWidth(700f)
        dialog.setHeight(300f)

        // Get the OK button
        val buttonTable = dialog.button("OK").padBottom(20f)

        // Customize the button
        val okButton = buttonTable.getCells().first().getActor() as TextButton
        okButton.pad(15f)
        okButton.label.setFontScale(1.5f)

        // Center the dialog on screen
        dialog.setPosition(
            (Gdx.graphics.width - dialog.width) / 2,
            (Gdx.graphics.height - dialog.height) / 2
        )

        dialog.show(stage)
        return dialog
    }

    /**
     * Shows a loading dialog with large text and improved size
     *
     * @param message The loading message to display
     * @return The dialog instance that was created
     */
    private fun showLoadingDialog(message: String): Dialog {
        // Create a Dialog with empty title
        val dialog = Dialog("", skin)

        // Create a label with enlarged text for the loading message
        val loadingLabel = Label(message, skin, "big")
        loadingLabel.setFontScale(2.5f)
        loadingLabel.setAlignment(Align.center)

        // Add the label with extra padding
        dialog.contentTable.add(loadingLabel).width(600f).pad(50f).row()

        // Set minimum dimensions for the dialog
        dialog.setWidth(700f)
        dialog.setHeight(300f)

        // Center the dialog on screen
        dialog.setPosition(
            (Gdx.graphics.width - dialog.width) / 2,
            (Gdx.graphics.height - dialog.height) / 2
        )

        dialog.show(stage)
        return dialog
    }
}
