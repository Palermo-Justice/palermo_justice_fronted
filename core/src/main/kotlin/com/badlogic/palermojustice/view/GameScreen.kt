package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.model.GameState
import com.mygame.model.com.badlogic.palermojustice.Player

class GameScreen(
    private val player: Player
) : Screen {

    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var waitingLabel: Label

    private var elapsed = 0f
    private var dotCount = 0
    private var dotsStarted = false

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

        val titleLabel = Label("YOU ARE...", skin, "title")
        val roleLabel = Label(player.role.name, skin, "title")
        val nameLabel = Label(player.name, skin, "default")
        waitingLabel = Label("", skin, "default")

        // Initial fade-in setup
        titleLabel.color.a = 0f
        roleLabel.color.a = 0f
        nameLabel.color.a = 0f
        waitingLabel.color.a = 0f

        mainTable.add(titleLabel).padBottom(20f).row()
        mainTable.add(roleLabel).padBottom(40f).row()
        mainTable.add(nameLabel).padBottom(40f).row()
        mainTable.add(waitingLabel).padTop(40f).row()

        // Fade in animations
        titleLabel.addAction(Actions.fadeIn(1f))
        roleLabel.addAction(Actions.sequence(Actions.delay(1f), Actions.fadeIn(1f)))
        nameLabel.addAction(Actions.sequence(Actions.delay(2f), Actions.fadeIn(1f)))
        waitingLabel.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeIn(1f)))
    }


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        elapsed += delta

        // Start showing dots only after 3.5 seconds
        if (elapsed >= 3.5f) {
            dotsStarted = true
        }

        if (dotsStarted && elapsed >= 3.5f + dotCount) {
            dotCount++
            waitingLabel.setText("Continuing" + ".".repeat(dotCount))
        }

        // Trigger transition after ~7 seconds (adjust if needed)
        if (elapsed >= 7f) {
            Main.instance.setScreen(RoleActionScreen())
        }

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

