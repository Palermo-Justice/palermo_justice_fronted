package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
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
 * Uses GameController to access player data and synchronize votes.
 */
class VotingScreen(private val currentPlayer: Player) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var voteStatusLabel: Label

    private var votedPlayerId: String? = null
    private var hasVoted = false
    private var isProcessingResult = false
    private var isTransitioning = false

    // Track who has voted
    private val votedPlayers = mutableSetOf<String>()

    // Flag for automatic progression
    private var shouldAutoProceed = false
    private var autoProgressTimer = 0f
    private val autoProgressDelay = 1.5f // Wait 1.5 seconds before automatically proceeding

    // Reference to the game controller
    private val gameController = GameController.getInstance()

    // Timer to periodically check vote status
    private var voteStatusTimer = 0f
    private val voteStatusInterval = 1f // Check every second

    // Test mode settings
    private val useTestMode = false
    private var autoVoteTimer = 0f
    private val autoVoteDelay = 3f // 3 seconds before auto voting

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        // Reset votes at the beginning of voting phase
        gameController.resetVotes { success ->
            if (success) {
                Gdx.app.log("VotingScreen", "Votes reset successfully")
            } else {
                Gdx.app.error("VotingScreen", "Failed to reset votes")
            }
        }

        createUI()

        // Start listening for vote updates
        listenForVotes()

        // Initial check for votes
        updateVotedPlayersFromModel()
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

        // Status label to see who has voted
        voteStatusLabel = Label("0/${gameController.model.getLivingPlayers().size} players have voted", skin, "alt")
        voteStatusLabel.setAlignment(Align.center)
        voteStatusLabel.setFontScale(2f)
        mainTable.add(voteStatusLabel).expandX().padBottom(20f).row()

        // Get living players from GameModel through GameController
        val livingPlayers = gameController.model.getLivingPlayers()
            .filter { it.id != currentPlayer.id } // Filter out the current player

        if (livingPlayers.isEmpty()) {
            mainTable.add(Label("No living players to vote for!", skin, "big")).padBottom(20f).row()

            val skipButton = TextButton("Skip Voting", skin)
            skipButton.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    skipVote()
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
                    if (!hasVoted) {
                        votedPlayerId = player.id

                        // Update UI to show selection
                        buttons.forEach { (p, btn) ->
                            btn.isDisabled = true
                            if (p == player) {
                                btn.setText("Selected")
                            }
                        }

                        promptLabel.setText("You voted for ${player.name}")
                        processVoteAction(currentPlayer)

                        // Send the vote
                        sendVote(player.id)
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
                skipVote()
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
     * Sends the vote for the selected player
     */
    private fun sendVote(targetPlayerId: String) {
        if (hasVoted) return

        hasVoted = true
        gameController.vote(targetPlayerId)

        // Update locally
        votedPlayers.add(currentPlayer.id)
        updateVoteStatus()
    }

    /**
     * Skips the vote (equivalent to voting null)
     */
    private fun skipVote() {
        if (hasVoted) return

        hasVoted = true
        gameController.vote(null)

        // Update locally
        votedPlayers.add(currentPlayer.id)
        updateVoteStatus()
    }

    private fun updateVoteStatus() {
        val livingPlayers = gameController.model.getLivingPlayers()
        voteStatusLabel.setText("${votedPlayers.size}/${livingPlayers.size} players have voted")

        Gdx.app.log("VotingScreen", "Vote status: ${votedPlayers.size}/${livingPlayers.size} players have voted")

        // Check if all living players have voted
        if (votedPlayers.size >= livingPlayers.size && livingPlayers.isNotEmpty()) {
            Gdx.app.log("VotingScreen", "All living players have voted, proceeding to count votes")
            shouldAutoProceed = true
        }
    }

    private fun updateVotedPlayersFromModel() {
        votedPlayers.clear()

        // Get all players who have voted from the model
        for (player in gameController.model.getPlayers()) {
            if (player.isAlive && player.voted) {
                votedPlayers.add(player.id)

                // If current player has already voted, update the UI state
                if (player.id == currentPlayer.id) {
                    hasVoted = true
                }
            }
        }

        // Update UI
        updateVoteStatus()
    }

    private fun listenForVotes() {
        // Listen for vote results from the controller
        gameController.listenForVotes { eliminatedPlayer, isTie ->
            if (isTransitioning) return@listenForVotes

            Gdx.app.log("VotingScreen", "Vote result received: eliminatedPlayer=${eliminatedPlayer?.name}, isTie=$isTie")
            proceedToNextScreen(eliminatedPlayer, isTie)
        }

        // Also listen for vote updates from Firebase to keep track of who has voted
        Main.instance.firebaseInterface.listenForVotes { votesMap ->
            if (isTransitioning) return@listenForVotes

            Gdx.app.postRunnable {
                // Update voted players from the votes map
                votedPlayers.clear()
                votedPlayers.addAll(votesMap.keys)

                Gdx.app.log("VotingScreen", "Vote update: ${votedPlayers.size} players have voted: ${votedPlayers}")

                // If current player's vote is in the map, they have voted
                if (votesMap.containsKey(currentPlayer.id)) {
                    hasVoted = true
                }

                // Update UI and check if all players have voted
                updateVoteStatus()
            }
        }
    }

    private fun processVoteAction(currentPlayer: Player?) {
        if (currentPlayer == null) return

        // Also update the local model
        currentPlayer.voted = true

        // Use updatePlayerAttribute to mark player as voted in Firebase
        Main.instance.firebaseInterface.updatePlayerAttribute(
            currentPlayer.id,
            "voted",
            true
        ) { success ->
            if (success) {
                Gdx.app.log("VotingScreen", "Successfully updated voted for player ${currentPlayer.id}")
            } else {
                Gdx.app.log("VotingScreen", "Error updating voted for player ${currentPlayer.id}")
            }
        }
    }

    /**
     * Proceeds to the next screen with the result
     */
    private fun proceedToNextScreen(eliminatedPlayer: Player?, isTie: Boolean) {
        if (isTransitioning) return

        isTransitioning = true
        Gdx.app.log("VotingScreen", "Proceeding to next screen with result: player=${eliminatedPlayer?.name}, isTie=$isTie")

        // If there's an eliminated player
        if (eliminatedPlayer != null) {
            Main.instance.setScreen(VotingResultScreen(eliminatedPlayer, currentPlayer))
        } else {
            // If there's a tie or no votes
            Main.instance.setScreen(VotingResultScreen(null, currentPlayer))
        }
    }

    /**
     * Processes the vote results and transitions to the next screen
     */
    private fun processVotesAndProceed() {
        if (isProcessingResult || isTransitioning) return

        isProcessingResult = true
        Gdx.app.log("VotingScreen", "Processing votes and proceeding to next screen")

        // Get the tally of votes and find eliminated player
        val voteResult = gameController.model.countVotes()
        val eliminatedPlayer = voteResult.first
        val isTie = voteResult.second

        if (eliminatedPlayer != null) {
            // Mark player as dead in the model
            eliminatedPlayer.isAlive = false

            // Update in Firebase
            Main.instance.firebaseInterface.updatePlayerAttribute(
                eliminatedPlayer.id,
                "isAlive",
                false
            ) { success ->
                if (success) {
                    Gdx.app.log("VotingScreen", "Successfully updated isAlive for player ${eliminatedPlayer.name}")
                } else {
                    Gdx.app.log("VotingScreen", "Error updating isAlive for player ${eliminatedPlayer.name}")
                }

                // Proceed to next screen
                proceedToNextScreen(eliminatedPlayer, isTie)
            }
        } else {
            // If no player eliminated, just proceed
            proceedToNextScreen(null, isTie)
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Periodically update the vote status
        voteStatusTimer += delta
        if (voteStatusTimer >= voteStatusInterval) {
            voteStatusTimer = 0f
            updateVotedPlayersFromModel()
        }

        // Handle auto-voting in test mode
        if (useTestMode && !hasVoted) {
            autoVoteTimer += delta
            if (autoVoteTimer >= autoVoteDelay) {
                // Auto vote for a random player
                val livingPlayers = gameController.model.getLivingPlayers()
                    .filter { it.id != currentPlayer.id } // Filter out current player

                if (livingPlayers.isNotEmpty()) {
                    // Choose a mafia player to vote for if possible (to advance the game)
                    val targetPlayer = livingPlayers.find { it.role?.name == "Mafioso" } ?: livingPlayers.first()

                    votedPlayerId = targetPlayer.id
                    hasVoted = true

                    Gdx.app.log("VotingScreen", "TEST MODE: Auto-voted for player ${targetPlayer.name} with role ${targetPlayer.role?.name}")

                    // Submit vote to the game controller
                    gameController.vote(targetPlayer.id)
                } else {
                    // Skip vote if no players to vote for
                    Gdx.app.log("VotingScreen", "TEST MODE: No players to vote for, skipping")
                    gameController.vote(null)
                    hasVoted = true
                }
            }
        }

        // Handle auto progression if needed
        if (shouldAutoProceed && !isProcessingResult && !isTransitioning) {
            autoProgressTimer += delta
            if (autoProgressTimer >= autoProgressDelay) {
                processVotesAndProceed()
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
