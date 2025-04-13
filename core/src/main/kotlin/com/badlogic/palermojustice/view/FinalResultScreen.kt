package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Player

class FinalResultScreen : Screen {
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

        val titleLabel = Label("Final Results", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(3f)
        mainTable.add(titleLabel).expandX().padBottom(20f).row()

        GameState.players.forEach { player ->
            val row = Table()

            val nameLabel = Label("${player.name}", skin, "default")
            nameLabel.setFontScale(5f)
            nameLabel.setAlignment(Align.left)

            val roleLabel = Label("${player.role.name}", skin, "default")
            roleLabel.setFontScale(5f)
            roleLabel.setAlignment(Align.right)

            row.add(nameLabel).expandX().left().padLeft(200f)
            row.add(roleLabel).expandX().right().padRight(200f)

            mainTable.add(row).expandX().fillX().padBottom(10f).row()
        }
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
