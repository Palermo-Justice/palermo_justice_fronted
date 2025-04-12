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
import com.badlogic.palermojustice.model.GameState
import com.badlogic.palermojustice.model.Player

class VotingScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private var votedPlayer: Player? = null

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

        val titleLabel = Label("VOTING", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(5f)
        mainTable.add(titleLabel).expandX().padBottom(20f).row()

        val promptLabel = Label("who will be killed?", skin, "default")
        promptLabel.setAlignment(Align.center)
        promptLabel.setFontScale(5f)
        mainTable.add(promptLabel).expandX().padBottom(30f).row()

        val buttons = mutableMapOf<Player, ImageButton>()
        val redTarget = Texture(Gdx.files.internal("redtarget.jpg"))
        val blackTarget = Texture(Gdx.files.internal("target.png"))

        GameState.players.forEach { player ->
            val row = Table()

            val voteLabel = Label("${player.name}", skin, "alt")
            voteLabel.setFontScale(5f)
            voteLabel.setAlignment(Align.left)

            val image = Image(blackTarget)
            val voteButton = ImageButton(image.drawable)

            voteButton.addListener {
                if (votedPlayer == null) {
                    votedPlayer = player
                    buttons.forEach { (p, btn) ->
                        btn.isDisabled = true
                        if (p == player) {
                            btn.style.imageUp = Image(redTarget).drawable
                            btn.image.drawable = Image(redTarget).drawable
                        }
                    }
                    promptLabel.setText("You voted for ${player.name}")
                }
                true
            }

            buttons[player] = voteButton

            row.add(voteLabel)
            row.add(voteButton).pad(10f).size(100f,100f)

            mainTable.add(row).expandX().fillX().padBottom(10f).row()
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
