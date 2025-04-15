package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport

class GameOverScreen(private val winner: String) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        skin = Skin(Gdx.files.internal("pj2.json"))
        createUI()
    }

    private fun createUI() {
        val rootTable = Table()
        rootTable.setFillParent(true)
        rootTable.align(Align.center) // vertically centers the content
        stage.addActor(rootTable)

        // This table holds just the inner content
        val contentTable = Table()

        val titleLabel = Label("GAME OVER", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(3.5f)
        contentTable.add(titleLabel).padBottom(20f).row()

        val promptText = when (winner.lowercase()) {
            "mafia" -> "The Mafioso has taken over the town!"
            "villagers" -> "The villagers have purged the mafia!"
            else -> "Nobody wins..."
        }

        val captionLabel = Label(promptText, skin, "narration")
        captionLabel.setAlignment(Align.center)
        captionLabel.setFontScale(3f)
        contentTable.add(captionLabel).padBottom(30f).row()

        val imagePath = when (winner.lowercase()) {
            "mafia" -> "mafia.jpg"
            "villagers" -> "villagers.jpg"
            else -> "draw.jpg"
        }

        val image = Image(Texture(Gdx.files.internal(imagePath)))
        image.setScaling(Scaling.fit)
        contentTable.add(image).width(800f).height(600f).row()

        // Add content table to rootTable and center everything
        rootTable.add(contentTable)
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
