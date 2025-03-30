package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main

class HomeScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        //val atlas = TextureAtlas(Gdx.files.internal("pj2.atlas"))
        skin = Skin(Gdx.files.internal("comic-ui.json"))

        createUI()
    }

    private fun createUI() {
        // main table for the entire screen
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // title table
        val headerTable = Table()
        val titleLabel = Label("PALERMO JUSTICE", skin, "title")
        titleLabel.setAlignment(Align.center)

        headerTable.add(titleLabel).expandX().align(Align.center)
        titleLabel.setFontScale(2f)

        val fileHandle = Gdx.files.internal("godfather.jpg")
        val godfatherTexture = Texture(fileHandle)
        val godfatherImage = Image(godfatherTexture)

        // main buttons
        val createGameButton = TextButton("CREATE GAME", skin)
        createGameButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(CreateGameScreen())
            }
        })

        val joinGameButton = TextButton("JOIN GAME", skin)
        joinGameButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(JoinGameScreen())
            }
        })

        //bottom table for roles and settings
        val bottomTable = Table()
        val rolesButton = TextButton("ROLES", skin)
        rolesButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(RolesScreen())
            }
        })

        val settingsButtonBottom = TextButton("SETTINGS", skin)
        settingsButtonBottom.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(SettingsScreen())
            }
        })

        bottomTable.add(rolesButton).width(450f).height(150f).padRight(30f)
        bottomTable.add(settingsButtonBottom).width(450f).height(150f)

        //put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).row()
        mainTable.add(godfatherImage).size(300f, 300f).padTop(30f).row()

        val subtitleLabel = Label("THE GODFATHER", skin)
        mainTable.add(subtitleLabel).padTop(20f).row()

        mainTable.add(createGameButton).width(650f).height(150f).padTop(50f).row()
        mainTable.add(joinGameButton).width(650f).height(150f).padTop(30f).row()
        mainTable.add(bottomTable).padTop(50f).padBottom(40f).row()

        //style and aspect
        createGameButton.pad(15f)
        joinGameButton.pad(15f)
        rolesButton.pad(15f)
        settingsButtonBottom.pad(15f)
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
