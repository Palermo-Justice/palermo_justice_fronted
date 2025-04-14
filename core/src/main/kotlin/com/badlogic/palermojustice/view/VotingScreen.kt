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

/**
 * Screen that allows players to vote to eliminate another player.
 * Uses GameController to access player data.
 */
class VotingScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private var votedPlayer: Player? = null
    private val gameController = GameController.getInstance()

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

        val promptLabel = Label("Who will be eliminated?", skin, "default")
        promptLabel.setAlignment(Align.center)
        promptLabel.setFontScale(5f)
        mainTable.add(promptLabel).expandX().padBottom(30f).row()

        val buttons = mutableMapOf<Player, ImageButton>()
        val redTarget = Texture(Gdx.files.internal("redtarget.jpg"))
        val blackTarget = Texture(Gdx.files.internal("target.png"))

        // Get living players from GameModel through GameController
        val livingPlayers = gameController.model.getLivingPlayers()

        livingPlayers.forEach { player ->
            val row = Table()

            val voteLabel = Label(player.name, skin, "alt")
            voteLabel.setFontScale(5f)
            voteLabel.setAlignment(Align.left)

            val image = Image(blackTarget)
            val voteButton = ImageButton(image.drawable)

            voteButton.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
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

                        // Submit vote to the game controller
                        gameController.vote(player.id)

                        // Add a confirm button to proceed after voting
                        addConfirmButton(mainTable)
                    }
                }
            })

            buttons[player] = voteButton

            row.add(voteLabel).expandX().left().padLeft(20f)
            row.add(voteButton).pad(10f).size(100f, 100f).right().padRight(20f)

            mainTable.add(row).expandX().fillX().padBottom(10f).row()
        }

        // Add skip button if needed (e.g., for testing)
        val skipButton = TextButton("Skip Vote", skin)
        skipButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                addConfirmButton(mainTable)
            }
        })

        // Add the skip button to a new row
        val buttonRow = Table()
        buttonRow.add(skipButton).width(200f).height(60f)
        mainTable.add(buttonRow).padTop(30f).row()
    }

    /**
     * Adds a confirmation button to proceed after voting
     */
    private fun addConfirmButton(table: Table) {
        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Process the vote result
                processVoteResult()

                // Move to next phase or screen
                navigateToNextScreen()
            }
        })

        // Add the confirm button to the table
        val confirmRow = Table()
        confirmRow.add(confirmButton).width(200f).height(60f)
        table.add(confirmRow).padTop(30f).row()
    }

    /**
     * Process the vote result
     */
    private fun processVoteResult() {
        // If a player was voted for, mark them as dead
        votedPlayer?.let { player ->
            player.die()

            // Here you would normally send this to the server
            // For now, just update the local model
            // This would be handled by the controller in a networked game
        }
    }

    /**
     * Navigate to the next screen based on game state
     */
    private fun navigateToNextScreen() {
        // Check if game is over based on voting result
        if (gameController.model.getMafiosi().isEmpty()) {
            // Citizens win
            Main.instance.setScreen(FinalResultScreen())
        } else if (gameController.model.getCitizens().isEmpty()) {
            // Mafia wins
            Main.instance.setScreen(FinalResultScreen())
        } else {
            // Game continues - go to night phase
            gameController.startNightPhase()
            Main.instance.setScreen(RoleActionScreen())
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
