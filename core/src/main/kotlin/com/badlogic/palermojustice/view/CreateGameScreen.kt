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

class CreateGameScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var gameNameField: TextField
    private lateinit var playerNameField: TextField
    private lateinit var playerCountSelectBox: SelectBox<String>

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()
    }

    private fun createUI() {
        // Main table
        val mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.top().padTop(20f).padLeft(20f).padRight(20f)
        stage.addActor(mainTable)

        // Title
        val titleLabel = Label("Create Game", skin, "title")
        titleLabel.setFontScale(3f)
        mainTable.add(titleLabel).left().padBottom(30f).row()

        // Game name field
        val gameNameLabel = Label("Game name", skin)
        gameNameLabel.setFontScale(5f)
        mainTable.add(gameNameLabel).left().padBottom(10f).row()

        gameNameField = TextField("", skin, "custom")
        mainTable.add(gameNameField).fillX().size(540f, 150f).padBottom(30f).row()

        // Select players number
        val playerCountLabel = Label("Choose number of players", skin)
        playerCountLabel.setFontScale(3f)
        mainTable.add(playerCountLabel).left().padBottom(10f).row()

        playerCountSelectBox = SelectBox<String>(skin, "big")
        playerCountSelectBox.setItems("3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        playerCountSelectBox.selected = "5"
        mainTable.add(playerCountSelectBox).left().width(200f).height(80f).padBottom(30f).row()

        // Player name field
        val playerNameLabel = Label("My player name", skin)
        playerNameLabel.setFontScale(3f)
        mainTable.add(playerNameLabel).left().padBottom(10f).row()

        playerNameField = TextField("", skin)
        mainTable.add(playerNameField).fillX().height(100f).padBottom(50f).row()

        // Lower buttons
        val buttonsTable = Table()

        val backButton = TextButton("Back", skin)
        backButton.pad(10f)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        val createButton = TextButton("Create", skin)
        createButton.pad(10f)
        createButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Game logic
                val gameName = gameNameField.text
                val playerCount = playerCountSelectBox.selected.toInt()
                val playerName = playerNameField.text


                Main.instance.setScreen(LobbyScreen())
            }
        })

        buttonsTable.add(backButton).width(150f).padRight(20f)
        buttonsTable.add(createButton).width(150f)

        mainTable.add(buttonsTable).fillX()
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
