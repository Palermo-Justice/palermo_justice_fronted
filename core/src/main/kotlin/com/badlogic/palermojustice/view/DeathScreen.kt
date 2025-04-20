package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main
import com.badlogic.palermojustice.model.Player

/**
 * Simple death screen shown to players who have been eliminated.
 * Allows them to return to the home screen.
 */
class DeathScreen(private val player: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    // Timer for auto-transition
    private var elapsed = 0f
    private val autoTransitionDelay = 10f  // 10 seconds before auto-transition
    private var isTransitioning = false

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

        // Dramatic title with animation
        val titleLabel = Label("YOU HAVE DIED", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(3f)
        titleLabel.setColor(0.9f, 0.2f, 0.2f, 0f) // Start transparent with red color
        titleLabel.addAction(Actions.sequence(
            Actions.delay(0.5f),
            Actions.fadeIn(1.5f)
        ))

        table.add(titleLabel).expandX().align(Align.center).padBottom(40f).row()

        // Death message
        val deathMessage = "You have been eliminated from the game."
        val messageLabel = Label(deathMessage, skin, "narration")
        messageLabel.setAlignment(Align.center)
        messageLabel.setFontScale(2.5f)
        messageLabel.setWrap(true)
        messageLabel.color.a = 0f // Start transparent
        messageLabel.addAction(Actions.sequence(
            Actions.delay(2f),
            Actions.fadeIn(1f)
        ))

        table.add(messageLabel).width(700f).expandX().align(Align.center).padBottom(40f).row()

        // Role info
        val roleInfo = "You were a ${player.role?.name ?: "Unknown role"}."
        val roleLabel = Label(roleInfo, skin, "big")
        roleLabel.setAlignment(Align.center)
        roleLabel.setFontScale(1.8f)
        roleLabel.color.a = 0f // Start transparent
        roleLabel.addAction(Actions.sequence(
            Actions.delay(3.5f),
            Actions.fadeIn(1f)
        ))

        table.add(roleLabel).expandX().align(Align.center).padBottom(40f).row()

        // Return button
        val returnButton = TextButton("Return to Main Menu", skin)
        returnButton.color.a = 0f // Start transparent
        returnButton.addAction(Actions.sequence(
            Actions.delay(4.5f),
            Actions.fadeIn(0.8f)
        ))

        returnButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                returnToHome()
            }
        })

        table.add(returnButton).size(300f, 100f).padTop(20f).row()

        // Auto-return message
        val autoReturnLabel = Label("Returning to main menu in ${autoTransitionDelay.toInt()} seconds", skin)
        autoReturnLabel.setAlignment(Align.center)
        autoReturnLabel.setFontScale(1.2f)
        autoReturnLabel.color.a = 0f // Start transparent
        autoReturnLabel.addAction(Actions.sequence(
            Actions.delay(5f),
            Actions.fadeIn(0.8f)
        ))

        table.add(autoReturnLabel).expandX().align(Align.center).padTop(20f).row()
    }

    private fun returnToHome() {
        if (isTransitioning) return

        isTransitioning = true

        // Fade out and return to home screen
        stage.addAction(Actions.sequence(
            Actions.fadeOut(1f),
            Actions.run {
                Main.instance.setScreen(HomeScreen())
            }
        ))
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Increment elapsed time
        elapsed += delta

        // Auto-transition after delay
        if (elapsed >= autoTransitionDelay && !isTransitioning) {
            returnToHome()
        }

        // Update the auto-return message
        if (elapsed < autoTransitionDelay && !isTransitioning) {
            val autoReturnLabel = ((stage.actors.first() as Table).getChild(4) as Label)
            val remainingSeconds = (autoTransitionDelay - elapsed).toInt()
            autoReturnLabel.setText("Returning to main menu in $remainingSeconds seconds")
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
