package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.model.GameStateHelper
import com.badlogic.palermojustice.model.Ispettore
import com.badlogic.palermojustice.model.Mafioso
import com.badlogic.palermojustice.model.Player

/**
 * Screen for role-specific night actions.
 * Uses GameController instead of direct access to GameStateHelper.
 */
class RoleActionScreen(private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private val gameController = GameController.getInstance()
    private val confirmedPlayers = mutableSetOf<String>()
    private lateinit var confirmCountLabel: Label
    var announcementText: String? = null

    // Store the selected player ID
    private var selectedPlayerId: String? = null

    // Current role being processed
    private var currentRoleName: String = ""

    // Flag for using test mode (auto-confirm)
    private val useTestMode = false

    // Timer for auto-confirm in test mode
    private var autoConfirmTimer = 0f
    private val autoConfirmDelay = 2f // 2 seconds delay

    // Flag for direct progression
    private var shouldAutoProceed = false
    private var autoProgressCounter = 0f

    // Flag to track if player has confirmed
    private var hasConfirmed = false

    // NEW: Synced confirmation count from server
    private var serverConfirmationCount = 0
    private var totalPlayersCount = 0

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Get the current role from GameStateHelper
        if (GameStateHelper.currentRoleIndex < GameStateHelper.roleSequence.size) {
            currentRoleName = GameStateHelper.roleSequence[GameStateHelper.currentRoleIndex]
            Gdx.app.log("RoleActionScreen", "Current role: $currentRoleName, index: ${GameStateHelper.currentRoleIndex}")
        } else {
            // Handle the case where we've processed all roles
            Gdx.app.log("RoleActionScreen", "All roles processed, moving to announcement")
            Main.instance.setScreen(AnnouncementScreen("Night actions complete!", currentPlayer))
            return
        }

        // NEW: Get total players count
        totalPlayersCount = gameController.model.getPlayers().size
        Gdx.app.log("RoleActionScreen", "Total players count: $totalPlayersCount")

        createUI()

        // Set up listener for confirmedPlayers updates from server
        setupConfirmationListener()

        // In test mode, auto-select a target player for action
        if (useTestMode) {
            val players = gameController.model.getPlayers()
            if (players.isNotEmpty()) {
                // Select the first player that isn't the current role player as the target
                val currentRolePlayer = findPlayerWithRole(currentRoleName)
                val targetPlayer = players.firstOrNull { it.id != currentRolePlayer?.id }

                if (targetPlayer != null) {
                    selectedPlayerId = targetPlayer.id
                    Gdx.app.log("RoleActionScreen", "Auto-selected target player: ${targetPlayer.name} for role $currentRoleName")
                }
            }
        }
    }

    // NEW: Set up listener for confirmation updates
    private fun setupConfirmationListener() {
        // Listen for game updates that might contain confirmation data
        Main.instance.firebaseInterface.listenForGameUpdates { gameData ->
            try {
                val confirmations = gameData["confirmations"] as? Map<*, *>
                val roleIndex = gameData["currentNightRoleIndex"] as? Number

                if (confirmations != null && roleIndex != null &&
                    roleIndex.toInt() == GameStateHelper.currentRoleIndex) {

                    val count = confirmations.size
                    Gdx.app.log("RoleActionScreen", "Received confirmation count from server: $count")

                    // Update UI on render thread
                    Gdx.app.postRunnable {
                        serverConfirmationCount = count
                        updateConfirmationCountUI()

                        // Check if all players have confirmed
                        if (serverConfirmationCount >= totalPlayersCount && !shouldAutoProceed) {
                            Gdx.app.log("RoleActionScreen", "All players confirmed (from server). Auto-proceeding.")
                            shouldAutoProceed = true
                        }
                    }

                    // Check if current player's ID is in the confirmations list
                    if (confirmations.containsKey(currentPlayer.id)) {
                        hasConfirmed = true
                    }
                }

                // Check for game phase transition
                val phase = gameData["currentPhase"] as? String
                if (phase == "DAY_DISCUSSION" || phase == "DAY_VOTING") {
                    // Game has moved to day phase, we should proceed to the next screen
                    Gdx.app.postRunnable {
                        proceedToNextScreen()
                    }
                }

            } catch (e: Exception) {
                Gdx.app.error("RoleActionScreen", "Error processing game update: ${e.message}")
            }
        }
    }

    private fun createUI() {
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // Get current player with this role
        Gdx.app.log("RoleActionScreen", "Current player with role $currentRoleName: ${currentPlayer?.name}")

        // Instruction label
        val instructionText = getRoleDescription(currentRoleName)
        val instructionLabel = Label(instructionText, skin, "big")
        val titleLabel = Label(currentRoleName, skin, "title")

        instructionLabel.setFontScale(2f)
        titleLabel.setFontScale(3f)
        instructionLabel.setAlignment(Align.center)
        mainTable.add(titleLabel).padBottom(40f).row()
        mainTable.add(instructionLabel).padBottom(20f).row()

        // Player selection list
        val playerGrid = Table()
        playerGrid.defaults().pad(10f).width(150f).height(100f)

        // Get all players from the GameModel
        val players = gameController.model.getPlayers()
        Gdx.app.log("RoleActionScreen", "Total players: ${players.size}")

        // ButtonGroup to allow only one selected at a time
        val buttonGroup = ButtonGroup<TextButton>()
        buttonGroup.setMinCheckCount(0)
        buttonGroup.setMaxCheckCount(1)

        players.forEachIndexed { index, player ->
            val roleName = player.role?.name ?: "Unknown"
            val buttonText = "${player.name}\n$roleName"
            val playerButton = TextButton(buttonText, skin, "select_player")

            playerButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    selectedPlayerId = player.id
                    Gdx.app.log("RoleActionScreen", "Selected player: ${player.name}")
                }
            })

            buttonGroup.add(playerButton)
            playerGrid.add(playerButton).width(250f).height(250f).pad(10f)
            if ((index + 1) % 3 == 0) playerGrid.row()
        }

        mainTable.add(playerGrid).padBottom(20f).row()

        // Confirm counter label
        confirmCountLabel = Label("0 / ${players.size} players confirmed", skin, "big")
        confirmCountLabel.setFontScale(2f)
        mainTable.add(confirmCountLabel).padBottom(20f).row()

        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!hasConfirmed) {
                    val currentPlayer = findPlayerWithRole(currentRoleName)
                    processConfirmAction(currentPlayer)
                } else {
                    showErrorDialog("You have already confirmed your action!")
                }
            }
        })

        mainTable.add(confirmButton).width(200f).padTop(20f).row()

        // Manual simulate button (for debugging)
        if (useTestMode) {
            val simulateButton = TextButton("AUTO-SIMULATE (Test)", skin)
            simulateButton.setColor(1f, 0f, 0f, 1f) // Red
            simulateButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    forceAutoConfirmAll()
                }
            })
            mainTable.add(simulateButton).width(300f).padTop(20f).row()
        }

        // Add a test mode indicator if in test mode
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE ACTIVE - Auto confirming in ${autoConfirmDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            mainTable.add(testModeLabel).padTop(20f).row()
        }
    }

    private fun forceAutoConfirmAll() {
        Gdx.app.log("RoleActionScreen", "FORCE: Auto confirming all players")

        // Force add all players to confirmed
        val players = gameController.model.getPlayers()
        for (player in players) {
            if (!confirmedPlayers.contains(player.id)) {
                confirmedPlayers.add(player.id)
                Gdx.app.log("RoleActionScreen", "FORCE: Added player ${player.name} to confirmed")
            }
        }

        // Update the UI
        updateConfirmationCountUI()

        // Process action for current role
        val targetPlayer = gameController.model.getPlayers()
            .find { it.id == selectedPlayerId }

        if (currentPlayer != null && targetPlayer != null) {
            Gdx.app.log("RoleActionScreen", "FORCE: Processing action for ${currentPlayer.name} targeting ${targetPlayer.name}")
            val result = GameStateHelper.processNightAction(currentPlayer, targetPlayer)

            if (result != null) {
                if (currentPlayer.role is Mafioso) {
                    announcementText = result
                    Gdx.app.log("RoleActionScreen", "FORCE: Mafioso action result: $result")
                } else if (currentPlayer.role is Ispettore) {
                    Gdx.app.log("RoleActionScreen", "FORCE: Ispettore action result: $result")
                }
            }
        }

        // Set flag to auto proceed
        shouldAutoProceed = true
    }

    private fun processConfirmAction(currentPlayer: Player?) {
        val targetPlayer = gameController.model.getPlayers()
            .find { it.id == selectedPlayerId }
        Gdx.app.log("RoleActionScreen", "Process confirm: target player: ${targetPlayer?.name}")

        // Check if we have both a current player and a selected player
        // Prevent duplicate confirms
        if (currentPlayer != null && !hasConfirmed) {
            // Mark local player as confirmed
            hasConfirmed = true

            // Add to local list for UI update
            confirmedPlayers.add(currentPlayer.id)
            updateConfirmationCountUI()

            // NEW: Send confirmation to Firebase
            val confirmData = mapOf(
                "type" to "PLAYER_CONFIRMATION",
                "playerId" to currentPlayer.id,
                "roleIndex" to GameStateHelper.currentRoleIndex,
                "targetId" to (targetPlayer?.id ?: "")
            )

            // Send confirmation to server
            Main.instance.firebaseInterface.sendMessage("PLAYER_CONFIRMATION", confirmData)

            // Only perform action if player has this role
            if (targetPlayer != null && currentPlayer.role?.name == currentRoleName) {
                val result = GameStateHelper.processNightAction(currentPlayer, targetPlayer)

                if (result != null && currentPlayer.role is Ispettore) {
                    Gdx.app.log("RoleActionScreen", "Ispettore result: $result")
                    showInfoDialog(result) {}
                    return
                }

                if (result != null && currentPlayer.role is Mafioso) {
                    announcementText = result
                    Gdx.app.log("RoleActionScreen", "Mafioso result: $result")
                    // No return here - we want to send this to other players

                    // Send kill action to server for other players to see
                    val killData = mapOf(
                        "type" to "KILL_ACTION",
                        "targetId" to targetPlayer.id,
                        "message" to result
                    )
                    Main.instance.firebaseInterface.sendMessage("PLAYER_ACTION", killData)
                }
            }
        } else if (hasConfirmed) {
            // Show error message
            showErrorDialog("You can only confirm once per night!")
        }
    }

    // NEW: Update the confirmation counter UI
    private fun updateConfirmationCountUI() {
        // Use the server confirmation count if available, otherwise use local count
        val count = if (serverConfirmationCount > 0) serverConfirmationCount else confirmedPlayers.size
        confirmCountLabel.setText("$count / $totalPlayersCount players confirmed")
    }

    private fun proceedToNextScreen() {
        Gdx.app.log("RoleActionScreen", "Proceeding to next screen")

        // Increment role index
        GameStateHelper.currentRoleIndex++
        Gdx.app.log("RoleActionScreen", "New role index: ${GameStateHelper.currentRoleIndex}")

        // Send updated role index to server
        val updateData = mapOf(
            "currentNightRoleIndex" to GameStateHelper.currentRoleIndex
        )
        Main.instance.firebaseInterface.sendMessage("GAME_STATE_UPDATE", updateData)

        if (GameStateHelper.currentRoleIndex < GameStateHelper.roleSequence.size) {
            Gdx.app.log("RoleActionScreen", "Moving to next role screen")
            Main.instance.setScreen(RoleActionScreen(currentPlayer))
        } else {
            // Reset role index for next night
            GameStateHelper.currentRoleIndex = 0

            // If we have an announcement text from a Mafioso action, use it
            val finalAnnouncementText = announcementText ?: "The night has passed."
            Gdx.app.log("RoleActionScreen", "Moving to announcement screen with text: $finalAnnouncementText")

            // Update game phase to day
            val phaseData = mapOf(
                "currentPhase" to "DAY_DISCUSSION",
                "currentNightRoleIndex" to 0,
                "confirmations" to emptyMap<String, Any>() // Clear confirmations
            )
            Main.instance.firebaseInterface.sendMessage("GAME_STATE_UPDATE", phaseData)

            Main.instance.setScreen(AnnouncementScreen(finalAnnouncementText, currentPlayer))
        }
    }

    /**
     * Find a player with the specified role
     */
    private fun findPlayerWithRole(roleName: String): Player? {
        return gameController.model.getPlayers()
            .filter { it.isAlive }
            .find { it.role?.name == roleName }
    }

    /**
     * Get the description for a role
     */
    private fun getRoleDescription(roleName: String): String {
        // Get the first player with this role to get the description
        val player = findPlayerWithRole(roleName)
        return player?.role?.description ?: "No action"
    }

    /**
     * Show an error dialog
     */
    private fun showErrorDialog(message: String) {
        val dialog = Dialog("Error", skin)
        dialog.contentTable.add(Label(message, skin, "big")).pad(20f)
        dialog.button("OK")
        dialog.show(stage)
    }

    private fun showInfoDialog(message: String, onClose: () -> Unit) {
        val dialog = Dialog("Information", skin)

        // Create a label with the message and increase font size
        val messageLabel = Label(message, skin, "big")
        messageLabel.setFontScale(2f) // Change to whatever size you want
        messageLabel.setWrap(true)
        messageLabel.setAlignment(Align.center)

        dialog.contentTable.add(messageLabel).width(500f).pad(20f).row()
        dialog.button("OK") {
            onClose()
        }
        dialog.show(stage)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Handle auto-confirm in test mode
        if (useTestMode && !shouldAutoProceed) {
            autoConfirmTimer += delta
            if (autoConfirmTimer >= autoConfirmDelay) {
                // Reset timer
                autoConfirmTimer = 0f
                Gdx.app.log("RoleActionScreen", "Auto-confirm timer reached, forcing auto-confirm")
                forceAutoConfirmAll()
            }
        }

        // Handle auto progression if needed
        if (shouldAutoProceed) {
            autoProgressCounter += delta
            if (autoProgressCounter >= 1.0f) { // 1 second delay before proceeding
                proceedToNextScreen()
            }
        }

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
