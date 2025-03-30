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
        // Inizializza lo stage
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        // Carica lo skin
        skin = Skin(Gdx.files.internal("comic-ui.json"))

        // Crea l'interfaccia utente
        createUI()
    }

    private fun createUI() {
        // Table principale
        val mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.top().padTop(20f).padLeft(20f).padRight(20f)
        stage.addActor(mainTable)

        // Titolo
        val titleLabel = Label("Create Game", skin, "title")
        mainTable.add(titleLabel).left().padBottom(30f).row()

        // Campo nome del gioco
        val gameNameLabel = Label("Game name", skin)
        gameNameLabel.setFontScale(1.2f)
        mainTable.add(gameNameLabel).left().padBottom(10f).row()

        gameNameField = TextField("", skin)
        mainTable.add(gameNameField).fillX().height(50f).padBottom(30f).row()

        // Selezione numero giocatori
        val playerCountLabel = Label("Choose number of players", skin)
        playerCountLabel.setFontScale(1.2f)
        mainTable.add(playerCountLabel).left().padBottom(10f).row()

        playerCountSelectBox = SelectBox<String>(skin)
        playerCountSelectBox.setItems("3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        playerCountSelectBox.selected = "5"
        mainTable.add(playerCountSelectBox).left().width(100f).height(50f).padBottom(30f).row()

        // Campo nome del giocatore
        val playerNameLabel = Label("My player name", skin)
        playerNameLabel.setFontScale(1.2f)
        mainTable.add(playerNameLabel).left().padBottom(10f).row()

        playerNameField = TextField("", skin)
        mainTable.add(playerNameField).fillX().height(50f).padBottom(50f).row()

        // Pulsanti in basso
        val buttonsTable = Table()

        val backButton = TextButton("Back", skin)
        backButton.pad(10f)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                // Torna alla schermata home
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
        // Pulisci lo schermo
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Aggiorna e disegna lo stage
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
