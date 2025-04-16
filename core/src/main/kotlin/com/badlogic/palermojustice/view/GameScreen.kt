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
import com.badlogic.palermojustice.controller.GameController
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
    private lateinit var mainTable: Table
    private var playerRole: String = "Unknown"
    private var currentPhase: String = "Waiting"

    // Reference to the game controller
    private val gameController = GameController.getInstance()
    private var currentPlayer: Player? = null

    // Animation variables added from colleague's version
    private var elapsed = 0f
    private var dotCount = 0
    private var lastDotCount = 0
    private var dotTimer = 0f
    private var dotsStarted = false
    private var roleAnimationComplete = false
    private var showRoleAnimation = true

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Set up the connection between this view and the controller
        gameController.view = this

        // Initialize with player data from the model
        initializePlayerData()

        createUI()

        // Start listening for game updates
        setupGameUpdates()
    }

    /**
     * Initialize player data from the game model
     */
    private fun initializePlayerData() {
        // Store the current player ID in the model
        gameController.model.roomId = roomId

        // Find or create the player in the model
        currentPlayer = gameController.model.getPlayerByName(playerName)
        println("assigned role: ${currentPlayer?.role?.name}")
        if (currentPlayer == null) {
            // If player doesn't exist yet, add them
            currentPlayer = gameController.model.addPlayerByName(playerName)
        }

        // Store current player ID in model for reference
        gameController.model.currentPlayerId = currentPlayer?.id ?: ""

        // Get the player's role if assigned
        currentPlayer?.role?.let { role ->
            playerRole = role.name
            gameController.model.currentPlayerRole = role
        }
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
        // Update the model with all game data
        gameController.updateGameState(gameData)

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
                val roleStr = player["role"] as? String ?: "Unknown"
                val isAlive = player["isAlive"] as? Boolean ?: true

                // Update local role variable
                playerRole = roleStr

                // Update UI for this player
                updatePlayerUI(roleStr, isAlive)
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
        mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.name = "mainTable"
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
        titleLabel.setFontScale(3f)
        roleLabel = Label(playerRole, skin, "title")
        roleLabel.setFontScale(3f)
        waitingLabel = Label("", skin, "big")
        waitingLabel.setFontScale(1.5f)

        // Initial fade-in setup
        titleLabel.color.a = 0f
        roleLabel.color.a = 0f
        waitingLabel.color.a = 0f

        table.add(titleLabel).padBottom(20f).row()
        table.add(roleLabel).padBottom(40f).row()
        table.add(waitingLabel).padTop(40f).row()

        // Fade in animations
        titleLabel.addAction(Actions.fadeIn(1f))
        roleLabel.addAction(Actions.sequence(Actions.delay(1f), Actions.fadeIn(1f)))
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

        // Add action buttons based on phase and role
        addActionButtons(actionsTable)

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

    /**
     * Add action buttons based on current phase and player role
     */
    private fun addActionButtons(actionsTable: Table) {
        when (currentPhase) {
            "NIGHT" -> {
                if (currentPlayer?.isAlive == true) {
                    val nightActionButton = TextButton("Perform Night Action", skin)
                    nightActionButton.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            Main.instance.setScreen(RoleActionScreen(currentPlayer!!))
                        }
                    })
                    actionsTable.add(nightActionButton).width(200f).height(50f).padBottom(10f).row()
                }
            }
            "DAY_DISCUSSION" -> {
                // Add day discussion phase buttons
                val discussionButton = TextButton("End Discussion", skin)
                discussionButton.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        // Only host can move to next phase
                        if (isHost) {
                            // Move to voting phase
                            val nextPhaseData = mapOf(
                                "currentPhase" to "DAY_VOTING"
                            )
                            Main.instance.firebaseInterface.sendMessage("GAME_STATE_UPDATE", nextPhaseData)
                        } else {
                            showMessage("Only the host can end the discussion phase")
                        }
                    }
                })
                actionsTable.add(discussionButton).width(200f).height(50f).padBottom(10f).row()
            }
            "DAY_VOTING" -> {
                if (currentPlayer?.isAlive == true) {
                    val voteButton = TextButton("Vote", skin)
                    voteButton.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            Main.instance.setScreen(VotingScreen(currentPlayer!!))
                        }
                    })
                    actionsTable.add(voteButton).width(200f).height(50f).padBottom(10f).row()
                }
            }
        }
    }

    /**
     * Show a message dialog
     */
    private fun showMessage(message: String) {
        val dialog = Dialog("", skin)
        dialog.contentTable.add(Label(message, skin)).pad(20f)
        dialog.button("OK")
        dialog.show(stage)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        try {
            if (showRoleAnimation && !roleAnimationComplete) {
                elapsed += delta

                if (elapsed >= 1.5f) {
                    dotsStarted = true
                }

                if (dotsStarted) {
                    dotTimer += delta

                    // Every 0.5 seconds, update the dots
                    if (dotTimer >= 0.5f) {
                        dotTimer = 0f
                        dotCount = (dotCount % 3) + 1

                        // Only update label if dotCount changed
                        if (dotCount != lastDotCount) {
                            waitingLabel.setText("Continuing" + ".".repeat(dotCount))
                            lastDotCount = dotCount
                        }
                    }
                }

                // Trigger transition after ~6 seconds
                if (elapsed >= 6f) {
                    roleAnimationComplete = true

                    // Semplifichiamo questa parte usando direttamente mainTable
                    // invece di cercare l'attore per nome
                    stage.addAction(Actions.sequence(
                        Actions.fadeOut(0.5f),
                        Actions.run {
                            Main.instance.setScreen(RoleActionScreen(currentPlayer!!))
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

        // Recreate the UI to update action buttons
        createGameUI(mainTable)
    }

    private fun updateActionsForNight() {
        // This is now handled in addActionButtons
    }

    private fun updateActionsForDay() {
        // This is now handled in addActionButtons
    }

    private fun updateActionsForVoting() {
        // This is now handled in addActionButtons
    }

    fun updateDisplay() {
        // Update current player from model
        currentPlayer = gameController.model.getPlayerByName(playerName)

        // Update the role based on the current player
        currentPlayer?.role?.let { role ->
            playerRole = role.name
            updatePlayerUI(role.name, currentPlayer?.isAlive ?: true)
        }

        // Update phase
        updatePhaseDisplay(currentPhase)
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
