package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.model.Player
import com.badlogic.palermojustice.model.GamePhase

class AnnouncementScreen(private val resultText: String, private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var timerLabel: Label
    private val gameController = GameController.getInstance()

    // Timer variables similar to GameScreen
    private var elapsed = 0f
    private val transitionDelay = 6f // Time before transitioning
    private var isTransitioning = false

    // Animation variables for consistency with GameScreen
    private var dotCount = 0
    private var lastDotCount = 0
    private var dotTimer = 0f
    private var dotsStarted = false

    // Flag to check if the current player was killed
    private val isCurrentPlayerKilled = resultText.contains(currentPlayer.name) &&
        (resultText.contains("was killed") ||
            resultText.contains("was eliminated"))

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

        // Title with fade-in animation like GameScreen
        val titleLabel = Label("Morning has come...", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(2.5f)
        titleLabel.color.a = 0f // Start transparent
        titleLabel.addAction(Actions.fadeIn(1f)) // Fade in animation

        table.add(titleLabel).expandX().align(Align.center).padBottom(30f).row()

        // Result Text with delayed fade-in
        val resultLabel = Label(resultText, skin, "narration")
        resultLabel.setAlignment(Align.center)
        resultLabel.setFontScale(5f)
        resultLabel.color.a = 0f // Start transparent
        resultLabel.addAction(Actions.sequence(
            Actions.delay(1f),
            Actions.fadeIn(1f)
        ))

        // If current player was killed, highlight the text in red
        if (isCurrentPlayerKilled) {
            resultLabel.setColor(0.9f, 0.3f, 0.3f, 0f) // Start transparent but red
        }

        table.add(resultLabel).expandX().align(Align.center).padBottom(40f).row()

        // Timer label with delayed appearance
        timerLabel = Label("", skin, "big")
        timerLabel.setAlignment(Align.center)
        timerLabel.color.a = 0f // Start transparent
        timerLabel.addAction(Actions.sequence(
            Actions.delay(3f),
            Actions.fadeIn(1f)
        ))

        table.add(timerLabel).expandX().align(Align.center).padTop(40f).row()
    }

    private fun proceedToNextScreen() {
        if (isTransitioning) return

        isTransitioning = true

        // Add fade out effect before transition
        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.run {
                // Check if the current player was killed
                if (isCurrentPlayerKilled || !currentPlayer.isAlive) {
                    // Player was killed, go to death screen
                    Main.instance.setScreen(DeathScreen(currentPlayer))
                } else {
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
                        gameController.model.setPhase(GamePhase.DAY_VOTING)
                        Main.instance.setScreen(VotingScreen(currentPlayer))
                    }
                }
            }
        ))
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            // Increment elapsed time
            elapsed += delta

            // Handle dots animation after 1.5 seconds
            if (elapsed >= 1.5f) {
                dotsStarted = true
            }

            if (dotsStarted) {
                dotTimer += delta

                // Every 0.5 seconds, update the dots
                if (dotTimer >= 0.5f) {
                    dotTimer = 0f
                    dotCount = (dotCount % 3) + 1

                    // Only update label if dotCount changed
                    if (dotCount != lastDotCount) {
                        timerLabel.setText("Continuing" + ".".repeat(dotCount))
                        lastDotCount = dotCount
                    }
                }
            }

            // Trigger transition after delay time
            if (elapsed >= transitionDelay && !isTransitioning) {
                proceedToNextScreen()
            }

            stage.act(delta)
            stage.draw()
        } catch (e: Exception) {
            // Log any exception for debugging
            Gdx.app.error("AnnouncementScreen", "Error during rendering: ${e.message}", e)
        }
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
