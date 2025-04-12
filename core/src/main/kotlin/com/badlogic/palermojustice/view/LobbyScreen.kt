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
import com.badlogic.palermojustice.controller.LobbyController

class LobbyScreen(
    private val roomId: String,
    private val playerName: String,
    private val isHost: Boolean = false,
    private val gameName: String = "Game Lobby"
) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var titleLabel: Label
    private lateinit var codeLabel: Label
    private lateinit var playersTable: Table

    // Controller for logic
    private lateinit var controller: LobbyController

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Initialize controller
        controller = LobbyController(
            Main.instance.firebaseInterface,
            roomId,
            playerName,
            isHost
        )
        controller.setView(this)

        createUI()
    }

    // Update player list in UI
    fun updatePlayersList(playersList: List<String>) {
        // Clear the table
        playersTable.clear()

        // Add all players
        playersList.forEach { name ->
            val playerLabel = Label(name, skin)
            // Different styling for current player
            if (name == playerName) {
                // Use an existing style in the skin
                playerLabel.style = skin.get("default", Label.LabelStyle::class.java)
            }
            playersTable.add(playerLabel).fillX().height(50f).padBottom(10f).row()
        }
    }

    private fun createUI() {
        // main table for the entire screen
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // header table
        val headerTable = Table()
        titleLabel = Label(gameName, skin, "title")
        titleLabel.setAlignment(Align.center)

        // back button
        val backButton = TextButton("BACK", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Clean up and disconnect before going back
                controller.disconnect()
                Main.instance.setScreen(CreateGameScreen())
            }
        })

        headerTable.add(backButton).padRight(20f).align(Align.left)
        headerTable.add(titleLabel).expandX().align(Align.center)

        // Room code section
        val codeTable = Table()
        val codeHeaderLabel = Label("ROOM CODE:", skin)
        codeLabel = Label(roomId, skin, "title")
        codeLabel.setFontScale(1.5f)

        codeTable.add(codeHeaderLabel).padRight(20f)
        codeTable.add(codeLabel)

        // Players section
        val playersHeaderLabel = Label("PLAYERS", skin, "title")

        // Create a scrollable table for players
        playersTable = Table()
        playersTable.left().top()

        val scrollPane = ScrollPane(playersTable, skin)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)

        // Buttons section
        val buttonsTable = Table()

        val rolesButton = TextButton("ROLES", skin)
        rolesButton.pad(10f)
        rolesButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(RolesScreen())
            }
        })

        val startButton = TextButton("START", skin)
        startButton.pad(10f)
        startButton.isDisabled = !controller.isHost() // Only the host can start the game
        startButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                controller.startGame()
            }
        })

        buttonsTable.add(rolesButton).width(150f).padRight(20f)
        buttonsTable.add(startButton).width(150f)

        // Create a "copy code" button
        val copyButton = TextButton("Copy Code", skin)
        copyButton.pad(10f)
        copyButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val code = controller.copyRoomCode()
                showMessage("Room code copied: $code")
            }
        })

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(20f).row()
        mainTable.add(codeTable).fillX().padBottom(10f).row()
        mainTable.add(copyButton).width(150f).padBottom(20f).row()
        mainTable.add(playersHeaderLabel).left().padBottom(10f).row()
        mainTable.add(scrollPane).expand().fill().padBottom(20f).row()
        mainTable.add(buttonsTable).fillX().padBottom(20f)

        // Initial update of players
        updatePlayersList(controller.getPlayersList())
    }

    // Show a message dialog
    fun showMessage(message: String) {
        val dialog = Dialog("", skin)
        dialog.contentTable.add(Label(message, skin)).pad(20f)
        dialog.button("OK")
        dialog.show(stage)
    }

    // Navigate to the game screen
    fun navigateToGameScreen(roomId: String, playerName: String, isHost: Boolean) {
        Main.instance.setScreen(GameScreen(roomId, playerName, isHost))
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
