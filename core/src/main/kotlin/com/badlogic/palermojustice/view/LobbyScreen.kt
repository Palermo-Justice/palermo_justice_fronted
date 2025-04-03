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

class LobbyScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()
    }

    private fun createUI() {
        // main table for the entire screen
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // header table
        val headerTable = Table()
        val titleLabel = Label("GAME NAME", skin, "title")
        titleLabel.setFontScale(3f)
        titleLabel.setAlignment(Align.center)

        // back button
        val backButton = TextButton("BACK", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(CreateGameScreen())
            }
        })

        headerTable.add(titleLabel).expandX().align(Align.center)

        //TODO the code must be dynamic
        val codeLabel = Label("Code", skin, "title")
        codeLabel.setFontScale(2f)
        //TODO Players will be added in a loop (add as many players as joined the game)
        val playerNameField = TextField("Player 1", skin)
        val playerNameField2 = TextField("Player 2", skin)

        val buttonsTable = Table()

        val rolesButton = TextButton("ROLES", skin)
        rolesButton.pad(10f)
        rolesButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // TODO roles screen for this game
                Main.instance.setScreen(RolesScreen())
            }
        })

        val startButton = TextButton("START", skin)
        startButton.pad(10f)
        startButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {

                Main.instance.setScreen(GameScreen())
            }
        })

        buttonsTable.add(rolesButton).size(450f, 150f).padRight(20f)
        buttonsTable.add(startButton).size(450f, 150f)

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(20f).row()
        mainTable.add(codeLabel).fillX().padTop(10f).padBottom(20f).row()
        mainTable.add(playerNameField).size(450f, 150f).padBottom(50f).row()
        mainTable.add(playerNameField2).size(450f, 150f).padBottom(50f).row()
        mainTable.add(buttonsTable).fillX().padBottom(200f).row()
        mainTable.add(backButton).width(450f).height(150f)
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
