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

/**
 * Screen that displays the final results of the game.
 * Now uses GameController to access player data instead of GameStateHelper.
 */
class FinalResultScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
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

        val titleLabel = Label("Final Results", skin, "title")
        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(3f)
        mainTable.add(titleLabel).expandX().padBottom(20f).row()

        // Display header row
        val headerRow = Table()
        val playerHeader = Label("Player", skin, "title")
        val roleHeader = Label("Role", skin, "title")
        val statusHeader = Label("Status", skin, "title")

        headerRow.add(playerHeader).expandX().left().padLeft(50f)
        headerRow.add(roleHeader).expandX().center()
        headerRow.add(statusHeader).expandX().right().padRight(50f)

        mainTable.add(headerRow).expandX().fillX().padBottom(20f).row()

        // Get players from GameModel through GameController
        val players = gameController.model.getPlayers()

        // Create a row for each player
        players.forEach { player ->
            val row = Table()

            val nameLabel = Label(player.name, skin, "default")
            nameLabel.setFontScale(1.5f)
            nameLabel.setAlignment(Align.left)

            val roleLabel = Label(player.role?.name ?: "Unknown", skin, "default")
            roleLabel.setFontScale(1.5f)
            roleLabel.setAlignment(Align.center)

            // Add status (alive/dead)
            val statusLabel = Label(if (player.isAlive) "Alive" else "Dead", skin, "default")
            statusLabel.setFontScale(1.5f)
            statusLabel.setAlignment(Align.right)

            // Change the color based on status
            if (!player.isAlive) {
                statusLabel.setColor(0.8f, 0.2f, 0.2f, 1f) // Red for dead
            } else {
                statusLabel.setColor(0.2f, 0.8f, 0.2f, 1f) // Green for alive
            }

            row.add(nameLabel).expandX().left().padLeft(50f)
            row.add(roleLabel).expandX().center()
            row.add(statusLabel).expandX().right().padRight(50f)

            mainTable.add(row).expandX().fillX().padBottom(10f).row()
        }

        // Add winner information
        val winnerTeam = determineWinningTeam()
        if (winnerTeam != null) {
            val winnerRow = Table()
            val winnerLabel = Label("Winners: $winnerTeam", skin, "title")
            winnerLabel.setFontScale(2f)
            winnerLabel.setAlignment(Align.center)
            winnerRow.add(winnerLabel).expandX().center()

            mainTable.add(winnerRow).expandX().fillX().padTop(30f).padBottom(30f).row()
        }

        // Add button to return to main menu
        val buttonRow = Table()
        val mainMenuButton = TextButton("Return to Main Menu", skin)
        mainMenuButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        buttonRow.add(mainMenuButton).width(250f).height(60f)
        mainTable.add(buttonRow).padTop(30f)
    }

    /**
     * Determines which team won based on surviving players
     */
    private fun determineWinningTeam(): String? {
        val livingPlayers = gameController.model.getLivingPlayers()
        if (livingPlayers.isEmpty()) {
            return null
        }

        val mafiosiAlive = gameController.model.getMafiosi().isNotEmpty()
        val citizensAlive = gameController.model.getCitizens().isNotEmpty()

        return when {
            mafiosiAlive && !citizensAlive -> "Mafia"
            !mafiosiAlive && citizensAlive -> "Citizens"
            else -> null // Game still ongoing
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
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
