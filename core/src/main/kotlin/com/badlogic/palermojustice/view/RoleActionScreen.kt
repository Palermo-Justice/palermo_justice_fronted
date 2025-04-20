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
import com.badlogic.palermojustice.model.Sgarrista
import com.badlogic.palermojustice.model.Paesano

/**
 * Screen for role-specific night actions.
 * Simplified version where each player sees only their role's action screen
 * and all players act simultaneously in a single night phase.
 */
class RoleActionScreen(private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private val gameController = GameController.getInstance()
    private val confirmedPlayers = mutableSetOf<String>()
    private lateinit var confirmCountLabel: Label
    var announcementText: String? = null
    private val playersToConfirm = mutableListOf<String>()
    private var pendingKilledPlayerId: String? = null

    // Store the selected player ID
    private var selectedPlayerId: String? = null

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

    // Flag for direct progression
    private var shouldAutoProceed = false
    private var autoProgressCounter = 0f

    // Flag for using test mode (auto-confirm)
    private val useTestMode = false

    // Timer for auto-confirm in test mode
    private var autoConfirmTimer = 0f
    private val autoConfirmDelay = 2f // 2 seconds delay

    // Static tracker for whether we're currently in the role action sequence
    companion object {
        // These static flags help prevent multiple listeners and transitions
        var activeInstance: RoleActionScreen? = null

        // Store night actions results to be shown in announcement screen
        var nightActionsResults = mutableListOf<String>()
    }

    override fun show() {
        // Set this as the active instance
        activeInstance = this

        gameController.model.getLivingPlayers().forEach { player ->
            playersToConfirm.add(player.id)
        }

        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Clear previous night actions results when starting a new night phase
        nightActionsResults.clear()

        // Create UI based on player's role
        createUI()

        // Notify MessageHandler that we're in role action phase
        // to prevent unwanted game state updates
        val messageHandler = gameController.getMessageHandler()
        messageHandler?.setProcessingRoleAction(true)

        // Register for confirmation updates
        listenForConfirmations()

        // In test mode, auto-select a target player for action
        if (useTestMode) {
            autoSelectTargetForTestMode()
        }

        // Immediately check if all players have already confirmed
        checkIfAllPlayersConfirmed()
    }

    private fun autoSelectTargetForTestMode() {
        val players = gameController.model.getPlayers()
        if (players.isNotEmpty()) {
            // Select the first player that isn't the current player as the target
            val targetPlayer = players.firstOrNull { it.id != currentPlayer.id }

            if (targetPlayer != null) {
                selectedPlayerId = targetPlayer.id
                Gdx.app.log("RoleActionScreen", "Auto-selected target player: ${targetPlayer.name} for player ${currentPlayer.name}")
            }
        }
    }

    /**
     * Check if all players have confirmed and if so, start the transition
     */
    private fun checkIfAllPlayersConfirmed() {
        var allConfirmed = true

        // Update the list of confirmed players
        confirmedPlayers.clear()

        for (playerId in playersToConfirm) {
            val player = gameController.model.getPlayers().find { it.id == playerId }
            if (player != null && player.confirmed) {
                confirmedPlayers.add(player.id)
            } else {
                allConfirmed = false
            }
        }

        // Update the counter text
        if (::confirmCountLabel.isInitialized) {
            confirmCountLabel.setText("${confirmedPlayers.size} / ${playersToConfirm.size} players confirmed")
        }

        // If all have confirmed, proceed to the next screen
        if (allConfirmed && playersToConfirm.isNotEmpty()) {
            Gdx.app.log("RoleActionScreen", "All players already confirmed, should proceed to next screen")
            shouldAutoProceed = true
        }
    }

    private fun createUI() {
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        confirmCountLabel = Label("${confirmedPlayers.size} / ${playersToConfirm.size} players confirmed", skin, "big")

        // Determine which role UI to show based on the current player's role
        val playerRole = currentPlayer.role

        // Title and instruction based on the player's role
        val titleText: String
        val instructionText: String

        if (playerRole != null) {
            when (playerRole) {
                is Mafioso -> {
                    titleText = "Mafioso"
                    instructionText = "Select a player to eliminate"
                }
                is Ispettore -> {
                    titleText = "Ispettore"
                    instructionText = "Select a player to investigate"
                }
                is Sgarrista -> {
                    titleText = "Sgarrista"
                    instructionText = "Select a player to protect"
                }
                is Paesano -> {
                    titleText = "Night Phase"
                    instructionText = "Select any player (for show)"
                }
                else -> {
                    titleText = "Night Phase"
                    instructionText = "Select any player (for show)"
                }
            }
        } else {
            // Fallback if role is null
            titleText = "Night Phase"
            instructionText = "Select any player (for show)"
        }

        // Create labels with the determined text
        val instructionLabel = Label(instructionText, skin, "big")
        val titleLabel = Label(titleText, skin, "title")

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
                playerGrid.add(playerButton).width(200f).height(200f).pad(10f)
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


        for (playerId in playersToConfirm) {
            val player = gameController.model.getPlayers().find { it.id == playerId }
            if (player != null && player.confirmed) {
                confirmedPlayers.add(player.id)
            }
        }

        if (confirmedPlayers.size >= playersToConfirm.size && playersToConfirm.isNotEmpty()) {
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

                // Process the appropriate action based on the player's role
                val targetPlayer = gameController.model.getPlayers()
                    .find { it.id == selectedPlayerId }

                if (targetPlayer != null) {
                    when (currentPlayer.role) {
                        is Mafioso -> {
                            processMafiosoAction(currentPlayer, targetPlayer)
                        }
                        is Ispettore -> {
                            processIspettoreAction(currentPlayer, targetPlayer)
                        }
                        is Sgarrista -> {
                            processSgarristaAction(currentPlayer, targetPlayer)
                        }
                        else -> {
                            // For other roles (like Paesano) - they don't do anything special
                            Gdx.app.log("RoleActionScreen", "No special action for role ${currentPlayer.role?.name}")
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

    private fun processMafiosoAction(mafioso: Player, target: Player) {
        if (!target.isAlive) return

        pendingKilledPlayerId = target.id

        val result = GameStateHelper.processNightAction(mafioso, target)

        if (result != null) {
            nightActionsResults.add(result)
        }

        Gdx.app.log("RoleActionScreen", "Mafioso action result: $result")

        // Process Mafioso's killing action
        // Add result to night actions results
        if (result != null) {
            nightActionsResults.add(result)
        }

        Gdx.app.log("RoleActionScreen", "Mafioso action result: $result")

        // Only update isAlive status if the player wasn't protected
        if (!target.isProtected) {
            // Update target player's isAlive status to false in Firebase
            Main.instance.firebaseInterface.updatePlayerAttribute(
                target.id,
                "isAlive",
                false
            ) { updateSuccess ->
                if (updateSuccess) {
                    Gdx.app.log("RoleActionScreen", "Successfully killed player ${target.name}")

                    target.isAlive = false
                } else {
                    Gdx.app.log("RoleActionScreen", "Failed to kill player ${target.name}")
                }
            }
        } else {
            Gdx.app.log("RoleActionScreen", "Player ${target.name} was protected and not killed")
            // Override the previous result with protection message
            if (nightActionsResults.isNotEmpty()) {
                nightActionsResults.removeLast()
            }
            nightActionsResults.add("${target.name} was protected!")
            pendingKilledPlayerId = null
        }
    }

    private fun processIspettoreAction(ispettore: Player, target: Player) {
        val result = GameStateHelper.processNightAction(ispettore, target)
        if (result != null) {
            Gdx.app.log("RoleActionScreen", "Ispettore result: $result")

            // Show dialog to the Ispettore player only
            Gdx.app.postRunnable {
                showInfoDialog(result) {}
            }
        }
    }

    //Available in V2 :)
    private fun processSgarristaAction(sgarrista: Player, target: Player) {
        // For Sgarrista (Protector) role
        val result = GameStateHelper.processNightAction(sgarrista, target)

        // Update the target player's isProtected status to true in Firebase
        Main.instance.firebaseInterface.updatePlayerAttribute(
            target.id,
            "isProtected",
            true
        ) { updateSuccess ->
            if (updateSuccess) {
                Gdx.app.log("RoleActionScreen", "Successfully protected player ${target.name}")

                // Also update local model
                target.isProtected = true

                if (result != null) {
                    Gdx.app.log("RoleActionScreen", "Sgarrista action result: $result")
                }
            } else {
                Gdx.app.log("RoleActionScreen", "Failed to protect player ${target.name}")
            }
        }
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
        val allConfirmed = confirmedPlayers.size >= playersToConfirm.size
        Gdx.app.log("RoleActionScreen", "Check all confirms: ${confirmedPlayers.size}/${playersToConfirm.size} confirmed, all confirmed? $allConfirmed")

        if (allConfirmed) {
            shouldAutoProceed = true
        }
    }

    private fun proceedToNextScreen() {
        if (isTransitioning) return

        isTransitioning = true
        Gdx.app.log("RoleActionScreen", "Proceeding to announcement screen")

        // Reset confirmations and protections before moving to announcement
        resetPlayerConfirmations {
            resetProtections {
                // Determine announcement text based on night actions
                val finalAnnouncementText = if (nightActionsResults.isNotEmpty()) {
                    // Join all results or take the most important one
                    nightActionsResults.lastOrNull() ?: "The night has passed."
                } else {
                    "The night has passed."
                }

                Gdx.app.log("RoleActionScreen", "Moving to announcement screen with text: $finalAnnouncementText")

                // Reset the role action processing flag
                val messageHandler = gameController.getMessageHandler()
                messageHandler?.setProcessingRoleAction(false)

                // Reset the active instance reference
                activeInstance = null

                // Move to announcement screen
                Main.instance.setScreen(AnnouncementScreen(finalAnnouncementText, currentPlayer))
            }
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

    private fun resetProtections(onComplete: () -> Unit) {
        // Get all players
        val players = gameController.model.getPlayers()
        var completedUpdates = 0

        if (players.isEmpty()) {
            onComplete()
            return
        }

        // Reset the isProtected status for each player
        players.forEach { player ->
            player.isProtected = false // Local reset

            Main.instance.firebaseInterface.updatePlayerAttribute(
                player.id,
                "isProtected",
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

        // Process action for current player's role
        val targetPlayer = gameController.model.getPlayers()
            .find { it.id == selectedPlayerId }

        if (targetPlayer != null) {
            Gdx.app.log("RoleActionScreen", "FORCE: Processing action for ${currentPlayer.name} targeting ${targetPlayer.name}")

            // Process based on player's role
            when (currentPlayer.role) {
                is Mafioso -> {
                    processMafiosoAction(currentPlayer, targetPlayer)
                }
                is Ispettore -> {
                    processIspettoreAction(currentPlayer, targetPlayer)
                }
                is Sgarrista -> {
                    processSgarristaAction(currentPlayer, targetPlayer)
                }
                else -> {
                    Gdx.app.log("RoleActionScreen", "FORCE: No special action for role ${currentPlayer.role?.name}")
                }
            }
        }

        // Check if all players have confirmed
        if (confirmedPlayers.size >= players.size) {
            // Set flag to auto proceed
            shouldAutoProceed = true
        }
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
        // Don't remove any references here
    }

    override fun dispose() {
        // Remove the reference to the active instance only if this is the active instance
        if (activeInstance == this) {
            activeInstance = null

            // Reset the role action processing flag
            val messageHandler = gameController.getMessageHandler()
            messageHandler?.setProcessingRoleAction(false)
        }

        stage.dispose()
        skin.dispose()
    }
}
