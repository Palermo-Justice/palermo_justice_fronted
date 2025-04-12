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
    private val playersList = mutableListOf<String>()

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Add the current player to the list
        playersList.add(playerName)

        createUI()

        // Start listening for player updates
        setupPlayerUpdates()
    }

    private fun setupPlayerUpdates() {
        // Listen for game updates from Firebase
        Main.instance.firebaseInterface.listenForGameUpdates { gameData ->
            Gdx.app.postRunnable {
                updatePlayersFromGameData(gameData)
            }
        }
    }

    private fun updatePlayersFromGameData(gameData: Map<String, Any>) {
        // Extract player data from gameData
        val playersMap = gameData["players"] as? Map<String, Any> ?: return

        // Clear current players list (except self)
        playersList.clear()
        playersList.add(playerName) // Keep self in the list

        // Add all players from data
        playersMap.forEach { (_, playerData) ->
            val player = playerData as? Map<String, Any> ?: return@forEach
            val name = player["name"] as? String ?: return@forEach
            if (name != playerName && !playersList.contains(name)) {
                playersList.add(name)
            }
        }

        // Update UI
        updatePlayersUI()
    }

    private fun updatePlayersUI() {
        // Clear the table
        playersTable.clear()

        // Add all players
        playersList.forEachIndexed { index, name ->
            val playerLabel = Label(name, skin)
            // Style the current player differently
            if (name == playerName) {
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
                Main.instance.firebaseInterface.disconnect()
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
        startButton.isDisabled = !isHost // Only the host can start the game
        startButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (isHost && playersList.size >= 3) { // Minimum 3 players to start
                    // Send start game message
                    val gameData = mapOf(
                        "state" to "RUNNING",
                        "players" to playersList
                    )
                    Main.instance.firebaseInterface.sendMessage("GAME_START", gameData)
                    Main.instance.setScreen(GameScreen(roomId, playerName, isHost))
                } else if (playersList.size < 3) {
                    showMessage("Need at least 3 players to start")
                }
            }
        })

        buttonsTable.add(rolesButton).width(150f).padRight(20f)
        buttonsTable.add(startButton).width(150f)

        // Create a "copy code" button
        val copyButton = TextButton("Copy Code", skin)
        copyButton.pad(10f)
        copyButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // This requires platform-specific implementation, just show a message for now
                showMessage("Room code copied: $roomId")
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
        updatePlayersUI()
    }

    private fun showMessage(message: String) {
        val dialog = Dialog("", skin)
        dialog.contentTable.add(Label(message, skin)).pad(20f)
        dialog.button("OK")
        dialog.show(stage)
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
