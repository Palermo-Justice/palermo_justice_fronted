package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.model.GameModel
import com.badlogic.palermojustice.model.Player

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
    private lateinit var waitingLabel: Label
    private lateinit var mainTable: Table  // Aggiungiamo un riferimento alla tabella principale
    private var playerRole: String = "Unknown"
    private var currentPhase: String = "Waiting"

    // Animation variables added from colleague's version
    private var elapsed = 0f
    private var dotCount = 0
    private var dotsStarted = false
    private var roleAnimationComplete = false
    private var showRoleAnimation = true

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
        Gdx.app.log("GameScreen", "Current phase: $phase")
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
        if (!roleAnimationComplete && showRoleAnimation) {
            // If we're in animation phase, don't update yet
            return
        }

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
        mainTable = Table()  // Inizializziamo la variabile mainTable
        mainTable.setFillParent(true)
        mainTable.name = "mainTable"  // Assegniamo un nome per poterla recuperare
        stage.addActor(mainTable)

        if (showRoleAnimation) {
            createRoleAnimationUI(mainTable)
        } else {
            createGameUI(mainTable)
        }
    }

    private fun createRoleAnimationUI(table: Table) {
        // Clear table if needed
        table.clear()

        // Role Animation UI based on colleague's code
        val titleLabel = Label("YOU ARE...", skin, "title")
        roleLabel = Label(playerRole, skin, "title")
        val nameLabel = Label(playerName, skin, "default")
        waitingLabel = Label("", skin, "default")

        // Initial fade-in setup
        titleLabel.color.a = 0f
        roleLabel.color.a = 0f
        nameLabel.color.a = 0f
        waitingLabel.color.a = 0f

        table.add(titleLabel).padBottom(20f).row()
        table.add(roleLabel).padBottom(40f).row()
        table.add(nameLabel).padBottom(40f).row()
        table.add(waitingLabel).padTop(40f).row()

        // Fade in animations
        titleLabel.addAction(Actions.fadeIn(1f))
        roleLabel.addAction(Actions.sequence(Actions.delay(1f), Actions.fadeIn(1f)))
        nameLabel.addAction(Actions.sequence(Actions.delay(2f), Actions.fadeIn(1f)))
        waitingLabel.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeIn(1f)))

        // Also create the game UI, but hide it for now
        statusLabel = Label("", skin)
        phaseLabel = Label(currentPhase, skin, "title")
    }

    private fun createGameUI(table: Table) {
        // Clear table if needed
        table.clear()

        // header table with room info
        val headerTable = Table()
        val roomLabel = Label("Room: $roomId", skin)
        headerTable.add(roomLabel).expandX().align(Align.left)

        // Role section
        roleLabel = Label("You are: $playerRole", skin, "title")
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
        table.add(headerTable).fillX().padTop(10f).padBottom(20f).row()
        table.add(roleLabel).fillX().padBottom(10f).row()
        table.add(statusLabel).fillX().padBottom(20f).row()
        table.add(phaseTable).fillX().padBottom(20f).row()
        table.add(actionsTable).fillX().expandY().top().padBottom(20f).row()
        table.add(leaveButton).width(150f).padBottom(20f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        try {
            if (showRoleAnimation && !roleAnimationComplete) {
                // Handle role animation logic
                elapsed += delta

                // Start showing dots only after 3.5 seconds
                if (elapsed >= 3.5f) {
                    dotsStarted = true
                }

                if (dotsStarted && elapsed >= 3.5f + dotCount * 0.5f) {
                    dotCount = (dotCount % 3) + 1
                    waitingLabel.setText("Continuing" + ".".repeat(dotCount))
                }

                // Trigger transition after ~7 seconds
                if (elapsed >= 7f) {
                    roleAnimationComplete = true

                    // Semplifichiamo questa parte usando direttamente mainTable
                    // invece di cercare l'attore per nome
                    stage.addAction(Actions.sequence(
                        Actions.fadeOut(0.5f),
                        Actions.run {
                            createGameUI(mainTable)  // Usiamo direttamente la referenza mainTable
                        },
                        Actions.fadeIn(0.5f)
                    ))
                }
            }

            stage.act(delta)
            stage.draw()
        } catch (e: Exception) {
            // Log any exception for debugging
            Gdx.app.error("GameScreen", "Error during rendering: ${e.message}", e)
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

    fun updatePhaseDisplay(phase: String) {
        if (!roleAnimationComplete && showRoleAnimation) {
            // If we're in animation phase, store for later
            currentPhase = phase
            return
        }

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
        playerRole = role

        if (roleAnimationComplete || !showRoleAnimation) {
            roleLabel.setText("You are: $role")
            // Additional role-specific UI setup
        } else {
            // Will be shown during animation
            roleLabel.setText(role)
        }
    }

    // Helper method to skip animation if needed
    fun skipRoleAnimation() {
        if (!roleAnimationComplete && showRoleAnimation) {
            roleAnimationComplete = true
            createGameUI(mainTable)
        }
    }
}
