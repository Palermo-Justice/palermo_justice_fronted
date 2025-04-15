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
class RoleActionScreen : Screen {
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

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Get the current role from GameStateHelper
        currentRoleName = GameStateHelper.roleSequence[GameStateHelper.currentRoleIndex]

        createUI()
    }

    private fun createUI() {
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // Get current player with this role
        val currentPlayer = findPlayerWithRole(currentRoleName)

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
                    println("Selected player: ${selectedPlayerId}")
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
                val targetPlayer = gameController.model.getPlayers()
                    .find { it.id == selectedPlayerId }
                    println("Target player: ${targetPlayer?.name}")

                // Check if we have both a current player and a selected player
                // Prevent duplicate confirms
                if (currentPlayer != null && !confirmedPlayers.contains(currentPlayer.id)) {
                    confirmedPlayers.add(currentPlayer.id)
                    confirmCountLabel.setText("${confirmedPlayers.size} / ${gameController.model.getPlayers().size} players confirmed")

                    // Only perform action if player has this role
                    if (targetPlayer != null && currentPlayer.role?.name == currentRoleName) {
                        val result = GameStateHelper.processNightAction(currentPlayer, targetPlayer)

                        if (result != null && currentPlayer.role is Ispettore) {
                            println("currentPlayer is ${(currentPlayer.name)}")
                            showInfoDialog(result) {}
                            return@clicked
                        }

                        if (result != null && currentPlayer.role is Mafioso) {
                            announcementText = result
                            return@clicked
                        }
                    }

                    // When everyone has confirmed, move to next role
                    if (confirmedPlayers.size == gameController.model.getPlayers().size) {
                        GameStateHelper.currentRoleIndex++

                        if (GameStateHelper.currentRoleIndex < GameStateHelper.roleSequence.size) {
                            Main.instance.setScreen(RoleActionScreen())
                        } else {
                            Main.instance.setScreen(announcementText?.let { AnnouncementScreen(it) })
                        }
                    }
                } else {
                    // Show error message
                    showErrorDialog("You can only confirm once per night!")
                }
            }
        })

        mainTable.add(confirmButton).width(200f).padTop(20f)
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
