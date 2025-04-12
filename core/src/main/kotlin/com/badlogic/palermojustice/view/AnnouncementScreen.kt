package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport

//!!note that a result text input needs to be passed
class AnnouncementScreen(private val resultText: String) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        skin = Skin(Gdx.files.internal("pj2.json"))
        createUI()
    }

    private fun createUI() {
        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        // Title
        val titleLabel = Label("Morning has come...", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(2.5f)
        table.add(titleLabel).expandX().align(Align.center).padBottom(30f).row()

        // Result Text
        val resultLabel = Label(resultText, skin, "narration")
        resultLabel.setAlignment(Align.center)
        resultLabel.setFontScale(5f)
        table.add(resultLabel).expandX().align(Align.center).padBottom(30f).row()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
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
