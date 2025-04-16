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

    // Flag to track if the current player has already confirmed
    private var hasConfirmed = false

    // Flag to prevent processing multiple updates at once
    private var isProcessingUpdate = false

    // Flag to prevent screen transitions during Firebase updates
    private var isTransitioning = false

    // Keep track of whether we've registered a listener to prevent duplicates
    private var hasRegisteredListener = false

    // Static tracker for whether we're currently in the role action sequence
    companion object {
        // These static flags help prevent multiple listeners and transitions
        var isInRoleActionSequence = false
        var activeRoleScreen: RoleActionScreen? = null
    }

    override fun show() {
        // Set this as the active role screen
        activeRoleScreen = this

        // Mark that we're in the role action sequence
        isInRoleActionSequence = true

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
                isInRoleActionSequence = false // Reset the sequence flag
                Main.instance.setScreen(AnnouncementScreen("Night actions complete!", currentPlayer))
            }
            return
        }

        createUI()

        // Register for confirmations only once
        if (!hasRegisteredListener) {
            listenForConfirmations()
            hasRegisteredListener = true
        }

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
        val players = gameController.model.getLivingPlayers() // Solo giocatori vivi
        Gdx.app.log("RoleActionScreen", "Total living players: ${players.size}")

        // ButtonGroup to allow only one selected at a time
        val buttonGroup = ButtonGroup<TextButton>()
        buttonGroup.setMinCheckCount(0)
        buttonGroup.setMaxCheckCount(1)

        players.forEachIndexed { index, player ->
            // Non mostrare il giocatore corrente nel grid delle selezioni
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
                playerGrid.add(playerButton).width(250f).height(250f).pad(10f)
                if ((index + 1) % 3 == 0) playerGrid.row()
            }
        }

        mainTable.add(playerGrid).padBottom(20f).row()

        // Confirm counter label
        confirmCountLabel = Label("0 / ${players.size} players confirmed", skin, "big")
        confirmCountLabel.setFontScale(2f)
        mainTable.add(confirmCountLabel).padBottom(20f).row()

        // Se il player ha già confermato (magari in una precedente istanza della schermata)
        // aggiorna l'UI per riflettere questo stato
        val isCurrentPlayerConfirmed = gameController.model.getPlayerByName(currentPlayer.name)?.confirmed ?: false
        hasConfirmed = isCurrentPlayerConfirmed

        val confirmButton = TextButton(if (hasConfirmed) "Confirmed" else "Confirm", skin)
        confirmButton.isDisabled = hasConfirmed

        confirmButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!hasConfirmed) {
                    // Aggiornare l'interfaccia immediatamente
                    confirmButton.setDisabled(true)
                    confirmButton.setText("Confirmed")

                    // Elaborare la conferma
                    processConfirmAction(currentPlayer)
                    hasConfirmed = true
                } else {
                    showErrorDialog("You have already confirmed!")
                }
            }
        })

        mainTable.add(confirmButton).width(200f).padTop(20f).row()

        // Aggiorna la lista dei giocatori confermati inizialmente (per riflettere lo stato attuale in Firebase)
        updateConfirmedPlayers()

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
     * Aggiorna la lista dei giocatori confermati in base ai dati attuali
     */
    private fun updateConfirmedPlayers() {
        confirmedPlayers.clear()

        // Aggiungi i giocatori che hanno già confermato
        gameController.model.getLivingPlayers()
            .filter { it.confirmed }
            .forEach { player ->
                confirmedPlayers.add(player.id)
            }

        // Aggiorna l'UI per mostrare i giocatori confermati
        val livingPlayers = gameController.model.getLivingPlayers()
        confirmCountLabel.setText("${confirmedPlayers.size} / ${livingPlayers.size} players confirmed")

        // Verifica se tutti hanno confermato
        if (confirmedPlayers.size >= livingPlayers.size && livingPlayers.isNotEmpty()) {
            shouldAutoProceed = true
            Gdx.app.log("RoleActionScreen", "All players already confirmed, proceeding...")
        }
    }

    private fun forceAutoConfirmAll() {
        Gdx.app.log("RoleActionScreen", "FORCE: Auto confirming all players")

        // Force add all players to confirmed
        val players = gameController.model.getLivingPlayers()
        for (player in players) {
            if (!confirmedPlayers.contains(player.id)) {
                confirmedPlayers.add(player.id)
                // Aggiorna anche il flag confirmed nel database
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

        // Verifica se tutti i giocatori hanno confermato
        if (confirmedPlayers.size >= players.size) {
            // Set flag to auto proceed
            shouldAutoProceed = true
        }
    }

    private fun processConfirmAction(currentPlayer: Player?) {
        if (currentPlayer == null) return

        // Aggiungi il giocatore corrente alla lista locale di quelli confermati
        confirmedPlayers.add(currentPlayer.id)

        val livingPlayers = gameController.model.getLivingPlayers()
        confirmCountLabel.setText("${confirmedPlayers.size} / ${livingPlayers.size} players confirmed")

        // Modifica: Usa direttamente updatePlayerAttribute per minimizzare gli aggiornamenti
        // e prevenire ricaricamenti multipli
        Main.instance.firebaseInterface.updatePlayerAttribute(
            currentPlayer.id,
            "confirmed",
            true
        ) { success ->
            if (success) {
                Gdx.app.log("RoleActionScreen", "Aggiornamento confirmed riuscito per player ${currentPlayer.id}")
                currentPlayer.confirmed = true

                // Esegui l'azione del ruolo solo se il giocatore ha questo ruolo
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
                Gdx.app.log("RoleActionScreen", "Errore nell'aggiornamento confirmed per player ${currentPlayer.id}")
            }
        }

        // Verifica se tutti i giocatori hanno confermato ora
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
        Gdx.app.log("RoleActionScreen", "Setting up confirmation listener")

        // IMPORTANTE: Usiamo un listener specifico che monitora solo i cambiamenti della "players" node
        // invece di ascoltare per tutti gli aggiornamenti del gioco
        Main.instance.firebaseInterface.listenForGameUpdates { gameData ->
            if (isProcessingUpdate || !isInRoleActionSequence) return@listenForGameUpdates

            // Verifica che questa sia ancora la schermata attiva prima di procedere
            if (activeRoleScreen != this) {
                Gdx.app.log("RoleActionScreen", "Update received but this is not the active role screen, ignoring")
                return@listenForGameUpdates
            }

            isProcessingUpdate = true

            Gdx.app.postRunnable {
                try {
                    // Estrai i dati dei giocatori
                    @Suppress("UNCHECKED_CAST")
                    val playersMap = gameData["players"] as? Map<String, Any> ?: run {
                        isProcessingUpdate = false
                        return@postRunnable
                    }

                    val newConfirmedPlayers = mutableSetOf<String>()
                    val livingPlayersCount = gameController.model.getLivingPlayers().size

                    // Processa ciascun giocatore
                    playersMap.forEach { (playerId, playerData) ->
                        if (playerData is Map<*, *>) {
                            val isAlive = playerData["isAlive"] as? Boolean ?: true
                            val isConfirmed = playerData["confirmed"] as? Boolean ?: false

                            if (isAlive && isConfirmed) {
                                newConfirmedPlayers.add(playerId)

                                // Aggiorna anche il modello locale
                                gameController.model.getPlayers()
                                    .find { it.id == playerId }
                                    ?.confirmed = isConfirmed
                            }
                        }
                    }

                    // Aggiorna solo se c'è un cambiamento
                    if (newConfirmedPlayers.size != confirmedPlayers.size) {
                        confirmedPlayers.clear()
                        confirmedPlayers.addAll(newConfirmedPlayers)

                        // Aggiorna UI
                        confirmCountLabel.setText("${confirmedPlayers.size} / $livingPlayersCount players confirmed")

                        // Verifica se tutti hanno confermato
                        if (confirmedPlayers.size >= livingPlayersCount && livingPlayersCount > 0) {
                            shouldAutoProceed = true
                            Gdx.app.log("RoleActionScreen", "Tutti i giocatori hanno confermato, procedendo...")
                        }
                    }
                } catch (e: Exception) {
                    Gdx.app.error("RoleActionScreen", "Error processing game update", e)
                } finally {
                    isProcessingUpdate = false
                }
            }
        }
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

        // Se non ci sono altri ruoli, resetta gli stati di conferma per tutti i giocatori
        if (GameStateHelper.currentRoleIndex >= GameStateHelper.roleSequence.size) {
            resetPlayerConfirmations {
                // If we have an announcement text from a Mafioso action, use it
                val finalAnnouncementText = announcementText ?: "The night has passed."
                Gdx.app.log("RoleActionScreen", "Moving to announcement screen with text: $finalAnnouncementText")

                // Resetta il flag di sequenza quando finisce la sequenza di ruoli
                isInRoleActionSequence = false
                activeRoleScreen = null

                Main.instance.setScreen(AnnouncementScreen(finalAnnouncementText, currentPlayer))
            }
        } else {
            // Se ci sono ancora ruoli da processare, passa al ruolo successivo
            // senza resettare le conferme in Firebase
            Gdx.app.log("RoleActionScreen", "Moving to next role screen")
            Main.instance.setScreen(RoleActionScreen(currentPlayer))
        }
    }

    private fun resetPlayerConfirmations(onComplete: () -> Unit) {
        // Ottieni tutti i giocatori
        val players = gameController.model.getPlayers()
        var completedUpdates = 0

        if (players.isEmpty()) {
            onComplete()
            return
        }

        // Resetta lo stato di conferma per ogni giocatore
        players.forEach { player ->
            player.confirmed = false // Reset locale

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
    }

    override fun dispose() {
        if (activeRoleScreen == this) {
            activeRoleScreen = null
        }

        stage.dispose()
        skin.dispose()
    }
}
