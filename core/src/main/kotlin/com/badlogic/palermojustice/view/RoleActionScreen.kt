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

        // Header
        val titleLabel = Label("YOU ARE...", skin, "title")
        titleLabel.setAlignment(Align.center)
        mainTable.add(titleLabel).expandX().align(Align.center).padBottom(20f).row()

        // Get current role and player
        val currentRole = GameState.roleSequence[GameState.currentRoleIndex]
        val currentPlayer = GameState.getPlayerByRole(currentRole)

        // Role label
        val roleLabel = Label(currentRole, skin, "default")
        roleLabel.setAlignment(Align.center)
        mainTable.add(roleLabel).expandX().align(Align.center).padBottom(40f).row()

        // Player selection list
        val list = List<String>(skin)
        list.setItems(*GameState.players.map { it.name }.toTypedArray())
        val scrollPane = ScrollPane(list, skin)
        mainTable.add(scrollPane).width(300f).height(200f).padBottom(20f).row()

        // Confirm button
        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener {
            val selectedName = list.selected
            val targetPlayer = GameState.players.find { it.name == selectedName }

            // If no player is selected, return early with false
            if (targetPlayer == null) {
                return@addListener false
            }
            // If this player actually has the role, perform the action
            //currentPlayer?.let {
               // if (it.role.name == currentRole) {
               //     it.role.performAction(GameState.players, targetPlayer)
               // }
            //}

            // Move to the next role
            GameState.currentRoleIndex++

            // If all roles have acted, end phase; otherwise, go to next role
            if (GameState.currentRoleIndex < GameState.roleSequence.size) {
                Main.instance.setScreen(LobbyScreen())
            } else {
                Main.instance.setScreen(LobbyScreen())
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
