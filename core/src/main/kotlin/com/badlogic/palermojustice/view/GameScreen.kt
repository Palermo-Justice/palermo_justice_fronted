package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport

class GameScreen : Screen {
    private val stage = Stage(ScreenViewport())
    private lateinit var atlas: TextureAtlas

    override fun show() {
        Gdx.input.inputProcessor = stage

        // 🔹 Carica l'Atlas
        atlas = TextureAtlas(Gdx.files.internal("home.atlas"))

        // 🔹 Immagine principale (Godfather)
        val godfatherImage = Image(atlas.findRegion("godfather"))

        // 🔹 Bottoni
        val createGameButton = ImageButton(TextureRegionDrawable(atlas.findRegion("CreateGame")))
        val joinGameButton = ImageButton(TextureRegionDrawable(atlas.findRegion("JoinGame")))
        val settingsButton = ImageButton(TextureRegionDrawable(atlas.findRegion("settings")))

        // 🔹 Creiamo la Table principale per centrare tutto
        val mainTable = Table()
        mainTable.debug = true  // Mostra i bordi della table per debug
        mainTable.setFillParent(true)  // Fa occupare alla Table tutto lo schermo
        mainTable.center()             // Centra tutti gli elementi

        // 🔹 Aggiungiamo gli elementi alla Table principale
        mainTable.add(godfatherImage).width(300f).height(250f).padBottom(50f).row()
        mainTable.add(createGameButton).width(300f).height(100f).padBottom(20f).row()
        mainTable.add(joinGameButton).width(300f).height(100f).padBottom(20f).row()

        // 🔹 Aggiunta del bottone delle impostazioni in alto a destra
        val settingsTable = Table()
        settingsTable.setFillParent(true)
        settingsTable.top().right()
        settingsTable.add(settingsButton).pad(20f)

        // 🔹 Aggiunta di entrambe le table allo stage
        stage.addActor(mainTable)
        stage.addActor(settingsTable)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f) // Sfondo bianco
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(Math.min(Gdx.graphics.deltaTime, 1 / 30f))
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun hide() {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        stage.dispose()
        atlas.dispose()
    }
}
