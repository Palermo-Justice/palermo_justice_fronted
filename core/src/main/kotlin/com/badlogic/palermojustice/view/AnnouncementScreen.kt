package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.model.Player

class AnnouncementScreen(private val resultText: String, private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    // Test mode settings
    private val useTestMode = true
    private var autoTransitionTimer = 0f
    private val autoTransitionDelay = 5f // 5 seconds before auto transition
    private val gameController = GameController.getInstance()

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

        // Add a continue button to manually proceed
        val continueButton = TextButton("Continue", skin)
        continueButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                proceedToNextScreen()
            }
        })
        table.add(continueButton).size(300f, 100f).padTop(30f).row()

        // Add test mode indicator
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE - Auto proceeding in ${autoTransitionDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            table.add(testModeLabel).padTop(20f).row()
        }
    }

    private fun proceedToNextScreen() {
        // Check if game is over
        val mafiosi = gameController.model.getMafiosi()
        val citizens = gameController.model.getCitizens()

        if (mafiosi.isEmpty()) {
            // Citizens win - game over
            Main.instance.setScreen(GameOverScreen("Villagers"))
        } else if (citizens.isEmpty()) {
            // Mafia wins - game over
            Main.instance.setScreen(GameOverScreen("Mafia"))
        } else {
            // Game continues - start voting phase
            gameController.model.setPhase(com.badlogic.palermojustice.model.GamePhase.DAY_VOTING)
            Main.instance.setScreen(VotingScreen(currentPlayer))
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Handle auto-transition in test mode
        if (useTestMode) {
            autoTransitionTimer += delta
            if (autoTransitionTimer >= autoTransitionDelay) {
                proceedToNextScreen()
            }
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
