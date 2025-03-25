package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
//import com.badlogic.palermojustice.SettingsManager

enum class Language(val code: String) {
    ENGLISH("en"),
    NORWEGIAN("no")
}

class SettingsScreen : Screen {

    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var font: BitmapFont

    private lateinit var languageSelectBox: SelectBox<Language>
    private lateinit var darkModeCheckBox: CheckBox

    override fun show() {
        // Create a stage to handle UI elements
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        // Load assets (you can use AssetManager for more complex resource management)
        font = BitmapFont()
        skin = Skin(Gdx.files.internal("comic-ui.json"))

        // Create UI elements
        createUI()
    }

    private fun createUI() {
        // Layout
        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        // Create UI elements
        val titleLabel = Label("Settings", skin)
        var backButton = TextButton("Back", skin)

        languageSelectBox = SelectBox(skin)
        languageSelectBox.setItems(*Language.values())
//        languageSelectBox.selected = Language.values().find { it.code == SettingsManager.settings.language }
//        languageSelectBox.addListener(object : ChangeListener() {
//            override fun changed(event: ChangeEvent, actor: Actor) {
//                SettingsManager.settings.language = languageSelectBox.selected.code
//                SettingsManager.saveSettings()
//            }
//        })

        darkModeCheckBox = CheckBox("Dark Mode", skin)
//        darkModeCheckBox.isChecked = SettingsManager.settings.isDarkMode
//        darkModeCheckBox.addListener(object : ChangeListener() {
//            override fun changed(event: ChangeEvent, actor: Actor) {
//                SettingsManager.settings.isDarkMode = darkModeCheckBox.isChecked
//                SettingsManager.saveSettings()
//            }
//        })

        // Add UI elements to the table
        table.add(titleLabel).padBottom(50f).row()
        table.add(Label("Language", skin)).left().row()
        table.add(languageSelectBox).padBottom(20f).fillX().row()
        table.add(darkModeCheckBox).padBottom(20f).left().row()
        table.add(backButton)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)

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
        font.dispose()
    }
}
