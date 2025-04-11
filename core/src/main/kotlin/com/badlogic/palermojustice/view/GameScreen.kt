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
import com.badlogic.palermojustice.model.GameModel

class GameScreen(
    private val roomId: String,
    private val playerName: String,
    private val isHost: Boolean = false
) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var roleLabel: Label
    private lateinit var phaseLabel: Label
    private lateinit var statusLabel: Label
    private var playerRole: String = "Unknown"
    private var currentPhase: String = "Waiting"

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()

        // Start listening for game updates
        setupGameUpdates()
    }

    private fun setupGameUpdates() {
        // Listen for game updates from Firebase
        Main.instance.firebaseInterface.listenForGameUpdates { gameData ->
            Gdx.app.postRunnable {
                handleGameUpdate(gameData)
            }
        }
    }

    private fun handleGameUpdate(gameData: Map<String, Any>) {
        // Extract data relevant to the current player and game state
        val phase = gameData["currentPhase"] as? String ?: "Unknown"
        currentPhase = phase

        // Update player-specific data (like role)
        val playersMap = gameData["players"] as? Map<String, Any> ?: mapOf()
        playersMap.forEach { (playerId, playerData) ->
            val player = playerData as? Map<String, Any> ?: return@forEach
            val name = player["name"] as? String ?: return@forEach

            if (name == playerName) {
                playerRole = player["role"] as? String ?: "Unknown"
                val isAlive = player["isAlive"] as? Boolean ?: true

                // Update UI for this player
                updatePlayerUI(playerRole, isAlive)
            }
        }

        // Update general game state UI
        updatePhaseDisplay(phase)
    }

    private fun updatePlayerUI(role: String, isAlive: Boolean) {
        roleLabel.setText("You are: $role")

        if (!isAlive) {
            statusLabel.setText("You are DEAD")
            statusLabel.style = skin.get("error", Label.LabelStyle::class.java)
        } else {
            statusLabel.setText("You are ALIVE")
            statusLabel.style = skin.get("default", Label.LabelStyle::class.java)
        }
    }

    private fun createUI() {
        // main table for the entire screen
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // header table with room info
        val headerTable = Table()
        val roomLabel = Label("Room: $roomId", skin)
        headerTable.add(roomLabel).expandX().align(Align.left)

        // Role section
        roleLabel = Label("You are...", skin, "title")
        roleLabel.setAlignment(Align.center)

        // Player status
        statusLabel = Label("", skin)
        statusLabel.setAlignment(Align.center)

        // Phase display
        val phaseTable = Table()
        val phaseHeaderLabel = Label("Current Phase:", skin)
        phaseLabel = Label(currentPhase, skin, "title")
        phaseTable.add(phaseHeaderLabel).padRight(10f)
        phaseTable.add(phaseLabel)

        // Game actions (will depend on the role and phase)
        val actionsTable = Table()
        val actionsLabel = Label("Actions:", skin, "title")
        actionsTable.add(actionsLabel).left().padBottom(10f).row()

        // Will be populated dynamically based on role and phase

        // Add a button to leave the game
        val leaveButton = TextButton("Leave Game", skin)
        leaveButton.pad(10f)
        leaveButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Clean up and disconnect
                Main.instance.firebaseInterface.disconnect()
                Main.instance.setScreen(HomeScreen())
            }
        })

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(20f).row()
        mainTable.add(roleLabel).fillX().padBottom(10f).row()
        mainTable.add(statusLabel).fillX().padBottom(20f).row()
        mainTable.add(phaseTable).fillX().padBottom(20f).row()
        mainTable.add(actionsTable).fillX().expandY().top().padBottom(20f).row()
        mainTable.add(leaveButton).width(150f).padBottom(20f)
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

    fun updatePhaseDisplay(phase: String) {
        phaseLabel.setText(phase)

        // Additional UI updates based on the phase
        when (phase) {
            "NIGHT" -> {
                // Night phase UI adjustments
                phaseLabel.style = skin.get("title", Label.LabelStyle::class.java)
                // Add night-specific actions based on role
                updateActionsForNight()
            }
            "DAY" -> {
                // Day phase UI adjustments
                phaseLabel.style = skin.get("default", Label.LabelStyle::class.java)
                // Add day-specific actions
                updateActionsForDay()
            }
            "VOTE" -> {
                // Voting phase UI adjustments
                // Add voting UI
                updateActionsForVoting()
            }
            else -> {
                // Default phase handling
            }
        }
    }

    private fun updateActionsForNight() {
        // Update actions based on player role during night
        // This will be implemented based on specific game rules
    }

    private fun updateActionsForDay() {
        // Update actions available during day phase
    }

    private fun updateActionsForVoting() {
        // Update actions for voting phase
    }

    fun updateDisplay() {
        // General display update logic
    }

    fun showRoleAssignment(role: String) {
        roleLabel.setText("You are: $role")
        // Additional role-specific UI setup
    }
}
