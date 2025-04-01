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

class JoinGameScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var nameField: TextField
    private lateinit var codeField: TextField

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("pj2.json"))

        createUI()
    }

    private fun createUI() {
        // main table
        val mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.top().padTop(20f).padLeft(20f).padRight(20f)
        stage.addActor(mainTable)

        // title
        val titleLabel = Label("JOIN GAME", skin, "title")
        titleLabel.setAlignment(Align.center)
        mainTable.add(titleLabel).expandX().center().padBottom(50f).row()

        // Name field table
        val nameTable = Table()
        val nameLabel = Label("NAME", skin)
        nameLabel.setFontScale(1.2f)
        nameTable.add(nameLabel).width(100f).padRight(10f)

        nameField = TextField("", skin)
        nameTable.add(nameField).fillX().height(50f)

        mainTable.add(nameTable).fillX().padBottom(30f).row()

        // Code field table
        val codeTable = Table()
        val codeLabel = Label("CODE", skin)
        codeLabel.setFontScale(1.2f)
        codeTable.add(codeLabel).width(100f).padRight(10f)

        codeField = TextField("", skin)
        codeTable.add(codeField).fillX().height(50f)

        mainTable.add(codeTable).fillX().padBottom(50f).row()

        // bottom buttons
        val buttonsTable = Table()

        val backButton = TextButton("BACK", skin)
        backButton.pad(10f)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        val joinButton = TextButton("JOIN", skin)
        joinButton.pad(10f)
        joinButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Logic to join a game
                val playerName = nameField.text
                val gameCode = codeField.text

                // TODO
                Main.instance.setScreen(HomeScreen())
            }
        })

        buttonsTable.add(backButton).width(150f).padRight(20f)
        buttonsTable.add(joinButton).width(150f)

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
