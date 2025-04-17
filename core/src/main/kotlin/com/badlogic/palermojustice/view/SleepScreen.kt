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
import com.badlogic.palermojustice.model.GameStateHelper
import com.badlogic.palermojustice.model.Player

class SleepScreen(private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    // Test mode settings
    private val useTestMode = false
    private var autoTransitionTimer = 0f
    private val autoTransitionDelay = 3f // 3 seconds before auto transition

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

        // Header
        val titleLabel = Label("Everyone goes to sleep...", skin, "narration")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(5f)
        mainTable.add(titleLabel).expandX().align(Align.center).padBottom(20f).row()

        // Image
        try {
            val sleepTexture = Texture(Gdx.files.internal("night.jpg"))
            val sleepImage = Image(sleepTexture)
            mainTable.add(sleepImage).align(Align.center).padTop(20f).row()
        } catch (e: Exception) {
            println("Error loading night.jpg image: ${e.message}")
            // Show placeholder if image is missing
            val placeholderLabel = Label("NIGHT", skin, "title")
            placeholderLabel.setFontScale(8f)
            placeholderLabel.setColor(0.1f, 0.1f, 0.3f, 1f)
            mainTable.add(placeholderLabel).align(Align.center).padTop(20f).row()
        }

        // Continue button
        val continueButton = TextButton("Continue", skin)
        continueButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                proceedToNightPhase()
            }
        })
        mainTable.add(continueButton).size(300f, 100f).padTop(40f).row()

        // Add test mode indicator
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE - Auto proceeding in ${autoTransitionDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            mainTable.add(testModeLabel).padTop(20f).row()
        }
    }

    private fun proceedToNightPhase() {
        // Reset night sequence before starting
        GameStateHelper.resetNightSequence()

        // Proceed to first role's action screen
        Main.instance.setScreen(RoleActionScreen(currentPlayer))
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Handle auto-transition in test mode
        if (useTestMode) {
            autoTransitionTimer += delta
            if (autoTransitionTimer >= autoTransitionDelay) {
                proceedToNightPhase()
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
