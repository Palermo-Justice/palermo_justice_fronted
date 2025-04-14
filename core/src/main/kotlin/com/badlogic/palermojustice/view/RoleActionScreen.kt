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
import com.badlogic.palermojustice.model.Player

/**
 * Screen for role-specific night actions.
 * Uses GameController instead of direct access to GameStateHelper.
 */
class RoleActionScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private val gameController = GameController.getInstance()

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
        val selectedPlayerId = arrayOf<String?>(null)

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
                    selectedPlayerId[0] = player.id
                    println("Selected player: ${player.name}")
                }
            })

            buttonGroup.add(playerButton)
            playerGrid.add(playerButton).width(250f).height(250f).pad(10f)
            if ((index + 1) % 3 == 0) playerGrid.row()
        }

        mainTable.add(playerGrid).padBottom(20f).row()

        // Confirm counter label
        val confirmCountLabel = Label("0 / ${players.size} players confirmed", skin, "big")
        confirmCountLabel.setFontScale(2f)
        mainTable.add(confirmCountLabel).padBottom(20f).row()

        // Keep track of who has confirmed
        val confirmedPlayers = mutableSetOf<String>()

        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val targetPlayer = gameController.model.getPlayers()
                    .find { it.id == selectedPlayerId[0] }

                // Check if we have both a current player and a selected player
                if (currentPlayer != null && targetPlayer != null) {
                    // Perform the role action
                    GameStateHelper.processNightAction(currentPlayer, targetPlayer)

                    // Move to next role in sequence
                    GameStateHelper.currentRoleIndex++

                    if (GameStateHelper.currentRoleIndex < GameStateHelper.roleSequence.size) {
                        // Go to next role's action screen
                        Main.instance.setScreen(RoleActionScreen())
                    } else {
                        // All night actions completed, go to next phase
                        // For testing, we'll go back to lobby
                        val hostName = gameController.model.getPlayers().firstOrNull()?.name ?: ""
                        Main.instance.setScreen(LobbyScreen("", hostName, true, ""))
                    }
                } else {
                    // Show error message
                    showErrorDialog("Please select a valid target")
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
