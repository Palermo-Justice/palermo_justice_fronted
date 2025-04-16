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
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.controller.GameController
import com.badlogic.palermojustice.model.Player

class VotingResultScreen(private val votedPlayer: Player? = null, private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private val gameController = GameController.getInstance()

    // Test mode settings
    private val useTestMode = false
    private var autoTransitionTimer = 0f
    private val autoTransitionDelay = 4f // 4 seconds before auto transition

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
            "${votedPlayer.name.uppercase()} was the ${votedPlayer.role?.name?.uppercase()}!"
        else
            "THE VOTES WAS A TIE!"

        val promptLabel = Label(promptText, skin, "default")
        promptLabel.setAlignment(Align.center)
        promptLabel.setFontScale(4f)
        mainTable.add(promptLabel).expandX().padBottom(30f).row()

        if (votedPlayer != null) {
            try {
                val image = Image(Texture(Gdx.files.internal("noose.jpg")))
                image.setScaling(Scaling.fit)
                mainTable.add(image).expandX().padBottom(30f).row()
            } catch (e: Exception) {
                println("Error loading noose.jpg image: ${e.message}")
                // Show something else if the image is missing
                val placeholderLabel = Label("X", skin, "title")
                placeholderLabel.setFontScale(10f)
                placeholderLabel.setColor(1f, 0f, 0f, 1f)
                mainTable.add(placeholderLabel).expandX().padBottom(30f).row()
            }
        }

        // Add continue button
        val continueButton = TextButton("Continue", skin)
        continueButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                proceedToNextScreen()
            }
        })
        mainTable.add(continueButton).size(300f, 100f).padTop(10f).row()

        // Add test mode indicator
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE - Auto proceeding in ${autoTransitionDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            mainTable.add(testModeLabel).padTop(20f).row()
        }
    }

    private fun proceedToNextScreen() {
        // Check if the game is over after voting
        if (gameController.model.getMafiosi().isEmpty()) {
            // Citizens win
            Main.instance.setScreen(GameOverScreen("Villagers"))
        } else if (gameController.model.getCitizens().isEmpty()) {
            // Mafia wins
            Main.instance.setScreen(GameOverScreen("Mafia"))
        } else {
            // Game continues - go to night phase
            gameController.startNightPhase()
            Main.instance.setScreen(SleepScreen(currentPlayer))
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
