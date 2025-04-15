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
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.controller.LobbyController
import com.badlogic.palermojustice.model.Mafioso
import com.badlogic.palermojustice.model.Paesano
import com.badlogic.palermojustice.model.Player

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

    // Store pending player updates until UI is ready
    private var pendingPlayersList: List<String>? = null

    // Controllers for logic
    private lateinit var controller: LobbyController
    private val gameController = GameController.getInstance()

    // Test players list for offline testing
    private val testPlayersList = mutableListOf<String>()
    private val useTestPlayers = true // Set to false to use real Firebase data

    override fun show() {
        // First initialize the UI
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Create test players for UI testing
        if (useTestPlayers) {
            setupTestPlayers()
        }

        // Create UI before controller initialization
        createUI()

        // Only after UI is ready, initialize controller
        controller = LobbyController(
            Main.instance.firebaseInterface,
            roomId,
            playerName,
            isHost
        )
        controller.setView(this)

        // If using test players, immediately update UI with them
        if (useTestPlayers) {
            updatePlayersList(testPlayersList)
        }
    }

    private fun setupTestPlayers() {
        // Add test players to the local list for UI
        testPlayersList.clear()
        testPlayersList.add(playerName) // Current player
        testPlayersList.add("Alice")
        testPlayersList.add("Bob")
        testPlayersList.add("Charlie")
        testPlayersList.add("Spongebob")

        // Clear existing players in the GameModel
        gameController.model.clearPlayers()

        // Add players to GameModel
        val currentPlayer = gameController.model.addPlayerByName(playerName)
        currentPlayer.role = Paesano()

        val alice = gameController.model.addPlayerByName("Alice")
        alice.role = Paesano()

        val bob = gameController.model.addPlayerByName("Bob")
        bob.role = Paesano()

        val charlie = gameController.model.addPlayerByName("Charlie")
        charlie.role = Paesano()

        val spongebob = gameController.model.addPlayerByName("Spongebob")
        spongebob.role = Mafioso()

        // Randomize roles for testing
        gameController.model.assignRoles()
    }

    // Update player list in UI
    fun updatePlayersList(playersList: List<String>) {
        // Check if UI is initialized
        if (!this::playersTable.isInitialized) {
            // Store the update for later
            pendingPlayersList = playersList
            return
        }

        // Clear the table
        playersTable.clear()

        // Add all players
        playersList.forEach { name ->
            val playerLabel = Label(name, skin, "big")
            playerLabel.setFontScale(1.5f)
            // Different styling for current player
            if (name == playerName) {
                // Use an existing style in the skin
                playerLabel.style = skin.get("big", Label.LabelStyle::class.java)
                // You might want to add some indicator that this is the current player
                val currentPlayerIndicator = Label(" (You)", skin, "big")
                currentPlayerIndicator.setFontScale(1.5f)
                val playerRow = Table()
                playerRow.add(playerLabel).left()
                playerRow.add(currentPlayerIndicator).left()
                playersTable.add(playerRow).fillX().height(50f).padBottom(10f).row()
            } else {
                playersTable.add(playerLabel).fillX().height(50f).padBottom(10f).row()
            }
        }

        // If no players were added (unlikely but possible), show a message
        if (playersList.isEmpty()) {
            playersTable.add(Label("No players connected", skin)).padTop(20f)
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
        titleLabel.setFontScale(3f)
        titleLabel.setAlignment(Align.center)

        // back button
        val backButton = TextButton("BACK", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Clean up and disconnect before going back
                if (this@LobbyScreen::controller.isInitialized) {
                    controller.disconnect()
                }
                Main.instance.setScreen(CreateGameScreen())
            }
        })

        headerTable.add(backButton).padRight(20f).align(Align.left)
        headerTable.add(titleLabel).expandX().align(Align.center)

        // Room code section
        val codeTable = Table()
        val codeHeaderLabel = Label("ROOM CODE:", skin, "big")
        codeHeaderLabel.setFontScale(1.5f)
        codeLabel = Label(roomId, skin, "title")
        codeLabel.setFontScale(1.5f)

        codeTable.add(codeHeaderLabel).padRight(20f)
        codeTable.add(codeLabel)

        // Players section
        val playersHeaderLabel = Label("PLAYERS", skin, "title")
        playersHeaderLabel.setFontScale(2f)

        // Create a scrollable table for players
        playersTable = Table()
        playersTable.left().top().padLeft(20f).padTop(10f)

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
                if (this@LobbyScreen::controller.isInitialized) {
                    controller.startGame()
                } else if (useTestPlayers) {
                    // Per test in offline mode
                    navigateToGameScreen(roomId, playerName, isHost)
                }
            }
        })

        buttonsTable.add(rolesButton).width(400f).height(200f).padRight(20f)
        buttonsTable.add(startButton).width(400f).height(200f)

        // Create a "copy code" button
        val copyButton = TextButton("Copy Code", skin)
        copyButton.pad(10f)
        copyButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (this@LobbyScreen::controller.isInitialized) {
                    val code = controller.copyRoomCode()
                    showMessage("Room code copied: $code")
                } else {
                    showMessage("Room code: $roomId")
                }
            }
        })

        // Debug info for test players
        if (useTestPlayers) {
            val debugInfoLabel = Label("RUNNING IN TEST MODE", skin)
            debugInfoLabel.setColor(1f, 0f, 0f, 1f) // Red color for testing
            mainTable.add(debugInfoLabel).padTop(5f).row()
        }

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(20f).row()
        mainTable.add(codeTable).fillX().padBottom(10f).row()
        mainTable.add(copyButton).width(200f).padBottom(20f).row()
        mainTable.add(playersHeaderLabel).left().padBottom(10f).padLeft(20f).row()
        mainTable.add(scrollPane).expand().fill().padBottom(20f).row()
        mainTable.add(buttonsTable).fillX().padBottom(20f)

        // Process any pending player updates
        pendingPlayersList?.let {
            updatePlayersList(it)
            pendingPlayersList = null
        }
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
