package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.model.Player
import com.badlogic.palermojustice.model.GamePhase

class VotingResultScreen(private val votedPlayer: Player? = null, private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private val gameController = GameController.getInstance()

    // Timer for automatic transition
    private var autoTransitionTimer = 0f
    private val autoTransitionDelay = 6f // 6 seconds before transition
    private var isTransitioning = false

    // Test mode flags
    private val useTestMode = false
    private var fastTransition = false // Fast transition for tests

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

        // Determine the result text
        val resultText = if (votedPlayer != null) {
            "${votedPlayer.name.uppercase()} was eliminated!"
        } else {
            "Nobody was eliminated."
        }

        val resultLabel = Label(resultText, skin, "narration")
        resultLabel.setAlignment(Align.center)
        resultLabel.setFontScale(4f)
        mainTable.add(resultLabel).expandX().padBottom(30f).row()

        // If there's an eliminated player, show their role
        if (votedPlayer != null) {
            val roleText = "${votedPlayer.name.uppercase()} was the ${votedPlayer.role?.name?.uppercase()}!"
            val roleLabel = Label(roleText, skin, "default")
            roleLabel.setAlignment(Align.center)
            roleLabel.setFontScale(3f)
            mainTable.add(roleLabel).expandX().padBottom(30f).row()

            // Try to show an appropriate image
            try {
                val image = Image(Texture(Gdx.files.internal("noose.jpg")))
                image.setScaling(Scaling.fit)
                mainTable.add(image).width(400f).height(300f).expandX().padBottom(30f).row()
            } catch (e: Exception) {
                Gdx.app.error("VotingResultScreen", "Error loading image: ${e.message}")
                // Show an alternative in case of error
                val placeholderLabel = Label("X", skin, "title")
                placeholderLabel.setFontScale(10f)
                placeholderLabel.setColor(1f, 0f, 0f, 1f)
                mainTable.add(placeholderLabel).expandX().padBottom(30f).row()
            }
        }

        // Add continue button (shown after a short delay)
        val continueButton = TextButton("Continue", skin)
        continueButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                proceedToNextScreen()
            }
        })

        // Initially hidden, show after a delay
        continueButton.color.a = 0f
        continueButton.addAction(Actions.sequence(
            Actions.delay(2f),
            Actions.fadeIn(0.5f)
        ))

        mainTable.add(continueButton).size(300f, 100f).padTop(20f).row()

        // Add test mode indicator if active
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE - Auto proceeding in ${if (fastTransition) 2 else autoTransitionDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            mainTable.add(testModeLabel).padTop(20f).row()
        }
    }

    /**
     * Proceeds to the next screen based on game state
     */
    private fun proceedToNextScreen() {
        if (isTransitioning) return

        isTransitioning = true

        // Add fade out effect before transition
        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.run {
                // Check if the game is over after voting
                checkGameStateAndContinue()
            }
        ))
    }

    /**
     * Checks the game state and proceeds to the appropriate next screen
     */
    private fun checkGameStateAndContinue() {
        // Check game state
        val mafiosi = gameController.model.getMafiosi()
        val citizens = gameController.model.getCitizens()

        Gdx.app.log("VotingResultScreen", "Checking game state: ${mafiosi.size} mafiosi, ${citizens.size} citizens")

        // If only 2 or fewer players remain and there's a mafioso, mafia wins
        if (mafiosi.isNotEmpty() && (mafiosi.size + citizens.size) <= 2) {
            // Mafia wins (harder to discover with only 2 players)
            Gdx.app.log("VotingResultScreen", "Game Over: Mafia wins (only 2 players left)")
            Main.instance.setScreen(GameOverScreen("Mafia"))
            return
        }

        // Check normal win conditions
        if (mafiosi.isEmpty()) {
            // Villagers win
            Gdx.app.log("VotingResultScreen", "Game Over: Villagers win (all mafia eliminated)")
            Main.instance.setScreen(GameOverScreen("Villagers"))
        } else if (citizens.isEmpty()) {
            // Mafia wins
            Gdx.app.log("VotingResultScreen", "Game Over: Mafia wins (all villagers eliminated)")
            Main.instance.setScreen(GameOverScreen("Mafia"))
        } else {
            // Game continues - start night phase
            Gdx.app.log("VotingResultScreen", "Game continues: starting night phase")
            gameController.startNightPhase()
            Main.instance.setScreen(SleepScreen(currentPlayer))
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Handle automatic transition
        autoTransitionTimer += delta
        if ((useTestMode && fastTransition && autoTransitionTimer >= 2f) ||
            (autoTransitionTimer >= autoTransitionDelay && !isTransitioning)) {
            proceedToNextScreen()
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
