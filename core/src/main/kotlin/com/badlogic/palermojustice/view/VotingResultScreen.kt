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
import com.badlogic.palermojustice.model.Player

//!! need to input the voted player (if any)
class VotingResultScreen(private val votedPlayer: Player? = null) : Screen {
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

        val titleLabel = Label("VOTING RESULTS", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(3f)
        mainTable.add(titleLabel).expandX().padBottom(20f).row()

        val captionText = if (votedPlayer != null)
            "${votedPlayer.name.uppercase()} was lynched!"
        else
            "Nobody was lynched."

        val captionLabel = Label(captionText, skin, "narration")
        captionLabel.setAlignment(Align.center)
        captionLabel.setFontScale(6f)
        mainTable.add(captionLabel).expandX().padBottom(30f).row()

        val promptText = if (votedPlayer != null)
            "${votedPlayer.name.uppercase()} was the ${votedPlayer.role.name.uppercase()}!"
        else
            "THE VOTES WAS A TIE!"

        val promptLabel = Label(promptText, skin, "default")
        promptLabel.setAlignment(Align.center)
        promptLabel.setFontScale(4f)
        mainTable.add(promptLabel).expandX().padBottom(30f).row()

        if (votedPlayer != null) {
            val image = Image(Texture(Gdx.files.internal("noose.jpg")))
            image.setScaling(Scaling.fit)
            mainTable.add(image).expandX().padBottom(30f).row()
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
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
