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

        // Get current role and player
        val currentRole = GameState.roleSequence[GameState.currentRoleIndex]
        val currentPlayer = GameState.players.firstOrNull { player ->
            player.role?.name == currentRole
        }

        // Instruction label
        val instructionText = currentPlayer?.role?.description ?: "No action"
        val instructionLabel = Label(instructionText, skin, "default")
        val titleLabel = Label(currentRole, skin, "title")

        instructionLabel.setAlignment(Align.center)
        mainTable.add(titleLabel).padBottom(40f).row()
        mainTable.add(instructionLabel).padBottom(20f).row()

        // Player selection list
        val playerGrid = Table()
        playerGrid.defaults().pad(10f).width(150f).height(100f)

        val selectedPlayerId = arrayOf<String?>(null) // mutable holder for selected player

        GameState.players.forEachIndexed { index, player ->
            val aliveStatus = if (player.isAlive) "Alive" else "Dead"
            val roleName = player.role?.name ?: "Unknown"
            val buttonText = "${player.name}\n$roleName - $aliveStatus"
            val playerButton = TextButton(buttonText, skin)

            playerButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    selectedPlayerId[0] = player.id
                    println("Selected player: ${player.name}")
                }
            })

            playerGrid.add(playerButton).width(150f).height(80f).pad(5f)
            if ((index + 1) % 3 == 0) playerGrid.row() // new row every 3 buttons
        }

        mainTable.add(playerGrid).padBottom(20f).row()

        // Confirm button
        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val targetPlayer = GameState.players.find { it.id == selectedPlayerId[0] }

                if (targetPlayer == null || currentPlayer == null) {
                    return
                }

                // Check if current player's role name matches the expected current role
                if (currentPlayer.role?.name != currentRole) {
                    return
                }

                currentPlayer.role?.performAction(GameState.players, targetPlayer)

                GameState.currentRoleIndex++

                if (GameState.currentRoleIndex < GameState.roleSequence.size) {
                    Main.instance.setScreen(RoleActionScreen())
                } else {
                    Main.instance.setScreen(LobbyScreen("", currentPlayer.name, true, "")) // or next phase
                }
            }
        })

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
