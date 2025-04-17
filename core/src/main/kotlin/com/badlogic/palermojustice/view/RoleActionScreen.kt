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
import com.badlogic.palermojustice.controller.MessageType
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
    private val useTestMode = true

    // Timer for auto-confirm in test mode
    private var autoConfirmTimer = 0f
    private val autoConfirmDelay = 2f // 2 seconds delay

    // Flag for direct progression
    private var shouldAutoProceed = false
    private var autoProgressCounter = 0f

    // Flag to track if the current player has already confirmed
    private var hasConfirmed = false

    // Flag to prevent processing multiple updates at once
    private var isProcessingUpdate = false

    // Flag to prevent screen transitions during Firebase updates
    private var isTransitioning = false

    // Flag to indicate that we're listening for confirmations
    private var isListeningForConfirmations = false

    // Timer to periodically check if all players have confirmed
    private var confirmationCheckTimer = 0f
    private val confirmationCheckInterval = 1f // Check every second

    // Static tracker for whether we're currently in the role action sequence
    companion object {
        // These static flags help prevent multiple listeners and transitions
        var activeInstance: RoleActionScreen? = null
    }

    override fun show() {
        // Set this as the active instance
        activeInstance = this

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
            if (!isTransitioning) {
                isTransitioning = true
                Main.instance.setScreen(AnnouncementScreen("Night actions complete!", currentPlayer))
            }
            return
        }

        createUI()

        // Notify MessageHandler that we're in role action phase
        // to prevent unwanted game state updates
        val messageHandler = gameController.getMessageHandler()
        messageHandler?.setProcessingRoleAction(true)

        // Register for confirmation updates
        listenForConfirmations()

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

        // Immediately check if all players have already confirmed
        checkIfAllPlayersConfirmed()
    }

    /**
     * Check if all players have confirmed and if so, start the transition
     */
    private fun checkIfAllPlayersConfirmed() {
        val livingPlayers = gameController.model.getLivingPlayers()
        var allConfirmed = true

        // Update the list of confirmed players
        confirmedPlayers.clear()

        livingPlayers.forEach { player ->
            if (player.confirmed) {
                confirmedPlayers.add(player.id)
            } else {
                allConfirmed = false
            }
        }

        // Update the counter text
        if (::confirmCountLabel.isInitialized) {
            confirmCountLabel.setText("${confirmedPlayers.size} / ${livingPlayers.size} players confirmed")
        }

        // If all have confirmed, proceed to the next screen
        if (allConfirmed && livingPlayers.isNotEmpty()) {
            Gdx.app.log("RoleActionScreen", "All players already confirmed, should proceed to next screen")
            shouldAutoProceed = true
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
        val players = gameController.model.getLivingPlayers() // Only living players
        Gdx.app.log("RoleActionScreen", "Total living players: ${players.size}")

        // ButtonGroup to allow only one selected at a time
        val buttonGroup = ButtonGroup<TextButton>()
        buttonGroup.setMinCheckCount(0)
        buttonGroup.setMaxCheckCount(1)

        // Check if the current player has already confirmed
        val isPlayerAlreadyConfirmed = currentPlayer.confirmed
        hasConfirmed = isPlayerAlreadyConfirmed

        // Update the list of confirmed players from the start
        updateConfirmedPlayersFromModel()

        // Add buttons to select target players
        var rowCount = 0
        players.forEach { player ->
            // Don't show the current player in the selection grid
            if (player.id != currentPlayer.id) {
                val buttonText = player.name
                val playerButton = TextButton(buttonText, skin, "select_player")

                playerButton.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        selectedPlayerId = player.id
                        Gdx.app.log("RoleActionScreen", "Selected player: ${player.name}")
                    }
                })

                buttonGroup.add(playerButton)
                playerGrid.add(playerButton).width(200f).height(120f).pad(10f)
                rowCount++
                if (rowCount % 3 == 0) playerGrid.row()
            }
        }

        mainTable.add(playerGrid).padBottom(20f).row()

        // Confirm counter label
        confirmCountLabel = Label("${confirmedPlayers.size} / ${players.size} players confirmed", skin, "big")
        confirmCountLabel.setFontScale(2f)
        mainTable.add(confirmCountLabel).padBottom(20f).row()

        val confirmButton = TextButton(if (hasConfirmed) "Confirmed" else "Confirm", skin)
        confirmButton.isDisabled = hasConfirmed

        confirmButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!hasConfirmed) {
                    // Update the interface immediately
                    confirmButton.isDisabled = true
                    confirmButton.setText("Confirmed")

                    // Process the confirmation
                    processConfirmAction(currentPlayer)
                    hasConfirmed = true
                } else {
                    showErrorDialog("You have already confirmed!")
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

    /**
     * Update the list of confirmed players using the model
     */
    private fun updateConfirmedPlayersFromModel() {
        confirmedPlayers.clear()

        // Add all living players who have already confirmed
        gameController.model.getLivingPlayers()
            .filter { it.confirmed }
            .forEach { player ->
                confirmedPlayers.add(player.id)
            }

        // If all players have confirmed, set the flag to proceed
        val livingPlayers = gameController.model.getLivingPlayers()
        if (confirmedPlayers.size >= livingPlayers.size && livingPlayers.isNotEmpty()) {
            shouldAutoProceed = true
        }
    }

    private fun forceAutoConfirmAll() {
        Gdx.app.log("RoleActionScreen", "FORCE: Auto confirming all players")

        // Force add all players to confirmed
        val players = gameController.model.getLivingPlayers()
        for (player in players) {
            if (!confirmedPlayers.contains(player.id)) {
                confirmedPlayers.add(player.id)
                // Also update the confirmed flag in the database
                setPlayerConfirmed(player.id, true)
                Gdx.app.log("RoleActionScreen", "FORCE: Added player ${player.name} to confirmed")
            }
        }

        // Update the UI
        confirmCountLabel.setText("${confirmedPlayers.size} / ${players.size} players confirmed")

        // Process action for current role if current player has the role
        if (currentPlayer.role?.name == currentRoleName) {
            val targetPlayer = gameController.model.getPlayers()
                .find { it.id == selectedPlayerId }

            if (targetPlayer != null) {
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
        }

        // Check if all players have confirmed
        if (confirmedPlayers.size >= players.size) {
            // Set flag to auto proceed
            shouldAutoProceed = true
        }
    }

    private fun processConfirmAction(currentPlayer: Player?) {
        if (currentPlayer == null) return

        // Add the current player to the local list of confirmed players
        confirmedPlayers.add(currentPlayer.id)

        val livingPlayers = gameController.model.getLivingPlayers()
        confirmCountLabel.setText("${confirmedPlayers.size} / ${livingPlayers.size} players confirmed")

        // Also update the local model
        currentPlayer.confirmed = true

        // Use updatePlayerAttribute directly to minimize updates
        // and prevent multiple reloads
        Main.instance.firebaseInterface.updatePlayerAttribute(
            currentPlayer.id,
            "confirmed",
            true
        ) { success ->
            if (success) {
                Gdx.app.log("RoleActionScreen", "Successfully updated confirmed for player ${currentPlayer.id}")

                // Execute role action only if the player has this role
                if (currentPlayer.role?.name == currentRoleName) {
                    val targetPlayer = gameController.model.getPlayers()
                        .find { it.id == selectedPlayerId }

                    if (targetPlayer != null) {
                        val result = GameStateHelper.processNightAction(currentPlayer, targetPlayer)

                        if (result != null && currentPlayer.role is Ispettore) {
                            Gdx.app.log("RoleActionScreen", "Ispettore result: $result")
                            Gdx.app.postRunnable {
                                showInfoDialog(result) {}
                            }
                            return@updatePlayerAttribute
                        }

                        if (result != null && currentPlayer.role is Mafioso) {
                            announcementText = result
                            Gdx.app.log("RoleActionScreen", "Mafioso result: $result")
                        }
                    }
                }
            } else {
                Gdx.app.log("RoleActionScreen", "Error updating confirmed for player ${currentPlayer.id}")
            }
        }

        // Check if all players have confirmed now
        checkAllConfirmsAndProceed()
    }

    private fun setPlayerConfirmed(playerId: String, confirmed: Boolean) {
        Main.instance.firebaseInterface.updatePlayerAttribute(
            playerId,
            "confirmed",
            confirmed
        ) { success ->
            if (success) {
                Gdx.app.log("RoleActionScreen", "Player $playerId confirmed status updated to $confirmed")
            }
        }
    }

    private fun listenForConfirmations() {
        if (isListeningForConfirmations) {
            Gdx.app.log("RoleActionScreen", "Already listening for confirmations, skipping")
            return
        }

        isListeningForConfirmations = true
        Gdx.app.log("RoleActionScreen", "Setting up confirmation listener")

        // Register a specific callback for confirmations
        val messageHandler = gameController.getMessageHandler()
        messageHandler?.registerCallback(MessageType.CONFIRMATION) { message ->
            if (isProcessingUpdate || activeInstance != this) return@registerCallback

            isProcessingUpdate = true

            Gdx.app.postRunnable {
                try {
                    // Extract confirmation data
                    @Suppress("UNCHECKED_CAST")
                    val confirmedData = message.payload as? Map<String, Any> ?: run {
                        isProcessingUpdate = false
                        return@postRunnable
                    }

                    // Update confirmed status of players in the model
                    updatePlayerConfirmationsFromData(confirmedData)

                    // Update UI
                    val livingPlayers = gameController.model.getLivingPlayers()
                    confirmCountLabel.setText("${confirmedPlayers.size} / ${livingPlayers.size} players confirmed")

                    // Check if all have confirmed
                    if (confirmedPlayers.size >= livingPlayers.size && livingPlayers.isNotEmpty()) {
                        shouldAutoProceed = true
                        Gdx.app.log("RoleActionScreen", "All players have confirmed, proceeding...")
                    }
                } catch (e: Exception) {
                    Gdx.app.error("RoleActionScreen", "Error processing confirmation update", e)
                } finally {
                    isProcessingUpdate = false
                }
            }
        }

        // Also check for direct Firebase updates
        Main.instance.firebaseInterface.listenForConfirmations { confirmedPlayerIds ->
            if (activeInstance != this) return@listenForConfirmations

            Gdx.app.postRunnable {
                val livingPlayers = gameController.model.getLivingPlayers()

                // Update the local list of confirmed players
                confirmedPlayers.clear()
                confirmedPlayers.addAll(confirmedPlayerIds)

                // Also update the local model
                gameController.model.getPlayers().forEach { player ->
                    player.confirmed = confirmedPlayerIds.contains(player.id)
                }

                // Update UI
                if (::confirmCountLabel.isInitialized) {
                    confirmCountLabel.setText("${confirmedPlayers.size} / ${livingPlayers.size} players confirmed")
                }

                // Check if all have confirmed
                if (confirmedPlayers.size >= livingPlayers.size && livingPlayers.isNotEmpty()) {
                    shouldAutoProceed = true
                    Gdx.app.log("RoleActionScreen", "All players confirmed, proceeding to next screen")
                }
            }
        }
    }

    /**
     * Update the confirmed status of players based on received data
     */
    private fun updatePlayerConfirmationsFromData(data: Map<String, Any>) {
        // If there is a players node, extract from there
        if (data.containsKey("players")) {
            @Suppress("UNCHECKED_CAST")
            val playersMap = data["players"] as? Map<String, Any> ?: return

            confirmedPlayers.clear()

            for ((playerId, playerData) in playersMap) {
                if (playerData is Map<*, *>) {
                    val isConfirmed = playerData["confirmed"] as? Boolean ?: false
                    val isAlive = playerData["isAlive"] as? Boolean ?: true

                    if (isAlive && isConfirmed) {
                        confirmedPlayers.add(playerId)

                        // Also update the model
                        gameController.model.getPlayers()
                            .find { it.id == playerId }
                            ?.confirmed = isConfirmed
                    }
                }
            }
        }

        // If there is a confirmations node, use that
        if (data.containsKey("confirmations")) {
            @Suppress("UNCHECKED_CAST")
            val confirmationsMap = data["confirmations"] as? Map<String, Boolean> ?: return

            for ((playerId, isConfirmed) in confirmationsMap) {
                if (isConfirmed) {
                    confirmedPlayers.add(playerId)

                    // Also update the model
                    gameController.model.getPlayers()
                        .find { it.id == playerId }
                        ?.confirmed = true
                }
            }
        }

        // Check if all have confirmed
        checkIfAllPlayersConfirmed()
    }

    private fun checkAllConfirmsAndProceed() {
        val livingPlayers = gameController.model.getLivingPlayers()
        val allConfirmed = confirmedPlayers.size >= livingPlayers.size
        Gdx.app.log("RoleActionScreen", "Check all confirms: ${confirmedPlayers.size}/${livingPlayers.size} confirmed, all confirmed? $allConfirmed")

        if (allConfirmed) {
            shouldAutoProceed = true
        }
    }

    private fun proceedToNextScreen() {
        if (isTransitioning) return

        isTransitioning = true
        Gdx.app.log("RoleActionScreen", "Proceeding to next screen")

        // Increment role index
        GameStateHelper.currentRoleIndex++
        Gdx.app.log("RoleActionScreen", "New role index: ${GameStateHelper.currentRoleIndex}")

        // If the role sequence is finished, reset confirmations and go to final screen
        if (GameStateHelper.currentRoleIndex >= GameStateHelper.roleSequence.size) {
            resetPlayerConfirmations {
                // If we have an announcement text from a Mafioso action, use it
                val finalAnnouncementText = announcementText ?: "The night has passed."
                Gdx.app.log("RoleActionScreen", "Moving to announcement screen with text: $finalAnnouncementText")

                // Reset the role action processing flag
                val messageHandler = gameController.getMessageHandler()
                messageHandler?.setProcessingRoleAction(false)

                // Reset the active instance reference
                if (activeInstance == this) {
                    activeInstance = null
                }

                Main.instance.setScreen(AnnouncementScreen(finalAnnouncementText, currentPlayer))
            }
        } else {
            // If there are still roles to process, don't reset confirmations
            Gdx.app.log("RoleActionScreen", "Moving to next role screen")
            Main.instance.setScreen(RoleActionScreen(currentPlayer))
        }
    }

    private fun resetPlayerConfirmations(onComplete: () -> Unit) {
        // Get all players
        val players = gameController.model.getPlayers()
        var completedUpdates = 0

        if (players.isEmpty()) {
            onComplete()
            return
        }

        // Reset the confirmed status for each player
        players.forEach { player ->
            player.confirmed = false // Local reset

            Main.instance.firebaseInterface.updatePlayerAttribute(
                player.id,
                "confirmed",
                false
            ) { success ->
                Gdx.app.postRunnable {
                    completedUpdates++
                    if (completedUpdates >= players.size) {
                        onComplete()
                    }
                }
            }
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

        // Periodically check if all players have confirmed
        confirmationCheckTimer += delta
        if (confirmationCheckTimer >= confirmationCheckInterval) {
            confirmationCheckTimer = 0f
            checkIfAllPlayersConfirmed()
        }

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

    override fun hide() {
        // Don't remove any references here because we might be just
        // transitioning to another role screen
    }

    override fun dispose() {
        // Remove the reference to the active instance only if this is the active instance
        if (activeInstance == this) {
            activeInstance = null

            // Reset the role action processing flag only if we're exiting
            // and not moving to another role
            if (GameStateHelper.currentRoleIndex >= GameStateHelper.roleSequence.size) {
                val messageHandler = gameController.getMessageHandler()
                messageHandler?.setProcessingRoleAction(false)
            }
        }

        stage.dispose()
        skin.dispose()
    }
}
