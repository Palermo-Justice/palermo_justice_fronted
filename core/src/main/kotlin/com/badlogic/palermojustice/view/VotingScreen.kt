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
class VotingScreen(private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private var votedPlayer: Player? = null
    private val gameController = GameController.getInstance()

    // Test mode settings
    private val useTestMode = false
    private var autoVoteTimer = 0f
    private val autoVoteDelay = 3f // 3 seconds before auto voting
    private var autoConfirmTimer = 0f
    private val autoConfirmDelay = 2f // 2 seconds before auto confirming
    private var hasVoted = false
    private var hasConfirmed = false

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

        // Get living players from GameModel through GameController
        val livingPlayers = gameController.model.getLivingPlayers()
            .filter { it.id != currentPlayer.id } // Filter out the current player

        if (livingPlayers.isEmpty()) {
            mainTable.add(Label("No living players to vote for!", skin, "big")).padBottom(20f).row()

            val skipButton = TextButton("Skip Voting", skin)
            skipButton.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    navigateToNextScreen()
                }
            })
            mainTable.add(skipButton).width(200f).height(60f).padTop(20f).row()
            return
        }

        val buttons = mutableMapOf<Player, TextButton>()
        val playersTable = Table()

        livingPlayers.forEach { player ->
            val row = Table()

            val voteLabel = Label(player.name, skin, "alt")
            voteLabel.setFontScale(3f)
            voteLabel.setAlignment(Align.left)

            val voteButton = TextButton("Vote", skin)
            voteButton.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    if (votedPlayer == null) {
                        votedPlayer = player

                        // Update UI to show selection
                        buttons.forEach { (p, btn) ->
                            btn.isDisabled = true
                            if (p == player) {
                                btn.setText("Selected")
                            }
                        }

                        promptLabel.setText("You voted for ${player.name}")
                        hasVoted = true

                        // Submit vote to the game controller
                        gameController.vote(player.id)

                        // Add a confirm button to proceed after voting
                        addConfirmButton(mainTable)
                    }
                }
            })

            buttons[player] = voteButton

            row.add(voteLabel).expandX().left().padLeft(20f)
            row.add(voteButton).pad(10f).size(150f, 60f).right().padRight(20f)

            playersTable.add(row).expandX().fillX().padBottom(10f).row()
        }

        mainTable.add(playersTable).expandX().fillX().padBottom(20f).row()

        // Add skip button
        val skipButton = TextButton("Skip Vote", skin)
        skipButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                hasVoted = true
                addConfirmButton(mainTable)
            }
        })

        // Add the skip button to a new row
        val buttonRow = Table()
        buttonRow.add(skipButton).width(200f).height(60f)
        mainTable.add(buttonRow).padTop(30f).row()

        // Add test mode indicator
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE - Auto voting in ${autoVoteDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            mainTable.add(testModeLabel).padTop(20f).row()
        }
    }

    /**
     * Adds a confirmation button to proceed after voting
     */
    private fun addConfirmButton(table: Table) {
        val confirmButton = TextButton("Confirm", skin)
        confirmButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                processVoteResult()
                hasConfirmed = true
                navigateToNextScreen()
            }
        })

        // Add the confirm button to the table
        val confirmRow = Table()
        confirmRow.add(confirmButton).width(200f).height(60f)
        table.add(confirmRow).padTop(30f).row()

        // Add test mode indicator for confirmation
        if (useTestMode) {
            val testModeLabel = Label("TEST MODE - Auto confirming in ${autoConfirmDelay.toInt()} seconds", skin, "big")
            testModeLabel.setColor(1f, 0f, 0f, 1f) // Red color
            table.add(testModeLabel).padTop(20f).row()
        }
    }

    /**
     * Process the vote result
     */
    private fun processVoteResult() {
        // If a player was voted for, mark them as dead
        votedPlayer?.let { player ->
            player.die()
            println("Player ${player.name} with role ${player.role?.name} has been eliminated by voting")
        }
    }

    /**
     * Navigate to the next screen based on game state
     */
    private fun navigateToNextScreen() {
        // First show voting result screen if we had a voted player
        if (votedPlayer != null) {
            Main.instance.setScreen(VotingResultScreen(votedPlayer, currentPlayer))
            return
        }

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
            Main.instance.setScreen(RoleActionScreen(currentPlayer))
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Handle auto-voting in test mode
        if (useTestMode) {
            if (!hasVoted) {
                autoVoteTimer += delta
                if (autoVoteTimer >= autoVoteDelay) {
                    // Auto vote for a random player
                    val livingPlayers = gameController.model.getLivingPlayers()
                        .filter { it.id != currentPlayer.id } // Filter out current player

                    if (livingPlayers.isNotEmpty()) {
                        // Choose a mafia player to vote for if possible (to advance the game)
                        val targetPlayer = livingPlayers.find { it.role?.name == "Mafioso" } ?: livingPlayers.first()

                        votedPlayer = targetPlayer
                        hasVoted = true

                        println("TEST MODE: Auto-voted for player ${targetPlayer.name} with role ${targetPlayer.role?.name}")

                        // Submit vote to the game controller
                        gameController.vote(targetPlayer.id)

                        // Add confirm button
                        addConfirmButton(stage.actors.first() as Table)
                    }
                }
            } else if (!hasConfirmed) {
                autoConfirmTimer += delta
                if (autoConfirmTimer >= autoConfirmDelay) {
                    processVoteResult()
                    hasConfirmed = true
                    navigateToNextScreen()
                }
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
