package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Role

class RoleActionScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()
    }

    private fun createUI() {
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        val currentRole = GameState.roleSequence[GameState.currentRoleIndex]
        val currentPlayer = GameState.players.firstOrNull {
            it.role.name == currentRole
        }

        val instructionText = currentPlayer?.role?.description ?: "No action"
        val instructionLabel = Label(instructionText, skin, "default")
        val titleLabel = Label(currentRole, skin, "title")
        instructionLabel.setAlignment(Align.center)

        mainTable.add(titleLabel).padBottom(40f).row()
        mainTable.add(instructionLabel).padBottom(20f).row()

        val playerGrid = Table()
        playerGrid.defaults().pad(10f).width(150f).height(100f)

        val selectedPlayerId = arrayOf<String?>(null)

        // ButtonGroup to allow only one selected at a time
        val buttonGroup = ButtonGroup<TextButton>()
        buttonGroup.setMinCheckCount(0)
        buttonGroup.setMaxCheckCount(1)

        GameState.players.forEachIndexed { index, player ->
            val aliveStatus = if (player.isAlive) "Alive" else "Dead"
            val buttonText = "${player.name}\n${player.role.name}"
            val playerButton = TextButton(buttonText, skin, "select_player")

            playerButton.addListener {
                selectedPlayerId[0] = player.id
                Gdx.app.log("DEBUG", "Selected player: ${player.name}")
                true
            }

            buttonGroup.add(playerButton)
            playerGrid.add(playerButton).width(150f).height(150f).pad(5f)
            if ((index + 1) % 3 == 0) playerGrid.row()
        }

        mainTable.add(playerGrid).padBottom(20f).row()

        // Confirm counter label
        val confirmCountLabel = Label("0 / ${GameState.players.size} players confirmed", skin)
        mainTable.add(confirmCountLabel).padBottom(20f).row()

        // Keep track of who has confirmed
        val confirmedPlayers = mutableSetOf<String>()

        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener {
            val targetPlayer = GameState.players.find { it.id == selectedPlayerId[0] }
            val currentPlayerId = GameState.players[0].id // <- You'll need a way to get the local player's ID

            if (targetPlayer == null) return@addListener false
            if (confirmedPlayers.contains(currentPlayerId)) return@addListener false

            // Perform the role action only if this player matches the current role
            val currentPlayer = GameState.players.find { it.id == currentPlayerId }
            if (currentPlayer?.role?.name == currentRole) {
                currentPlayer.role.performAction(GameState.players, targetPlayer)
            }

            confirmedPlayers.add(currentPlayerId)
            confirmCountLabel.setText("${confirmedPlayers.size} / ${GameState.players.size} players confirmed")

            if (confirmedPlayers.size >= GameState.players.size) {
                GameState.currentRoleIndex++
                if (GameState.currentRoleIndex < GameState.roleSequence.size) {
                    Main.instance.setScreen(RoleActionScreen())
                } else {
                    Main.instance.setScreen(LobbyScreen()) // or next phase
                }
            }

            true
        }

        mainTable.add(confirmButton).width(200f).padTop(20f)
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
