package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.model.GameModel

class GameScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()
    }

    private fun createUI() {
        // main table for the entire screen
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // header table
        val headerTable = Table()
        val titleLabel = Label("YOU ARE...", skin, "title")
        titleLabel.setAlignment(Align.center)

        headerTable.add(titleLabel).expandX().align(Align.center)

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(20f).row()

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

    fun updatePhaseDisplay(night: Any) {
        //TODO
    }

    fun updateDisplay() {
        TODO("Not yet implemented")
    }

    fun showRoleAssignment(role: GameModel) {
        //TODO implement and maybe correct role: GameModel should be something else (like Role)
    }
}
