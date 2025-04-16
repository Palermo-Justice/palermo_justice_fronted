package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.palermojustice.Main

enum class Language(val code: String) {
    ENGLISH("en"),
    NORWEGIAN("no")
}

class SettingsScreen : Screen {

    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var font: BitmapFont

    // For keeping track of the credits dialog
    private var creditsDialog: Dialog? = null

    override fun show() {
        // Create a stage to handle UI elements
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        // Load assets (you can use AssetManager for more complex resource management)
        font = BitmapFont()
        skin = Skin(Gdx.files.internal("pj2.json"))

        // Create UI elements
        createUI()
    }

    private fun createUI() {
        // Layout
        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        // Create UI elements
        val titleLabel = Label("SETTINGS", skin)
        titleLabel.setFontScale(5f)
        var backButton = TextButton("BACK", skin)

        // Credits button
        val creditsButton = TextButton("CREDITS", skin)
        creditsButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                showCreditsDialog()
            }
        })

        // When pressed, backButton (go back) redirects to home screen
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        // Add UI elements to the table
        table.add(titleLabel).padBottom(50f).row()
        table.add(creditsButton).size(450f, 150f).padBottom(20f).row()
        table.add(backButton).size(450f, 150f).padBottom(100f).row()

        // App version label at the bottom
        val versionLabel = Label("Palermo Justice v1.0.0", skin)
        versionLabel.setFontScale(1.5f)
        table.add(versionLabel).padTop(20f).row()
    }

    private fun showCreditsDialog() {
        // Close previous dialog if it exists
        creditsDialog?.hide()

        // Create a new dialog for the credits
        creditsDialog = Dialog("CREDITS", skin)

        creditsDialog?.width = 900f
        creditsDialog?.height = 700f

        // Container for the credits content
        val creditsContent = Table()
        creditsContent.pad(20f)

        // Add the game title
        val gameTitle = Label("Palermo Justice", skin, "title")
        gameTitle.setFontScale(1.8f)
        creditsContent.add(gameTitle).padBottom(20f).row()

        // Developed by
        val developedByLabel = Label("Developed by", skin)
        developedByLabel.setFontScale(1.5f)
        creditsContent.add(developedByLabel).padTop(20f).row()

        // Team name/members
        val teamTable = Table()
        val teamMembers = arrayOf(
            "Eiichiro Oda",
            "Spongebob",
            "Goku",
            "Teletubbies",
            "Walter White",
            "Los Pollos Hermanos"
        )

        teamMembers.forEach { member ->
            val memberLabel = Label(member, skin)
            memberLabel.setFontScale(1.2f)
            teamTable.add(memberLabel).row()
        }

        creditsContent.add(teamTable).padTop(10f).padBottom(20f).row()

        // Academic info
        val courseLabel = Label("TDT4240 - Software Architecture", skin)
        courseLabel.setFontScale(1.3f)
        creditsContent.add(courseLabel).padTop(20f).row()

        val universityLabel = Label("NTNU - Spring 2025", skin)
        universityLabel.setFontScale(1.3f)
        creditsContent.add(universityLabel).padTop(5f).padBottom(20f).row()


        // Add credits content to a ScrollPane in case it's long
        val scrollPane = ScrollPane(creditsContent, skin)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)

        // Add scrollPane to the dialog content table
        creditsDialog?.contentTable?.add(scrollPane)?.width(600f)?.height(500f)

        // Add OK button
        creditsDialog?.button("OK")

        // Show the dialog
        creditsDialog?.show(stage)
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
        font.dispose()
        creditsDialog = null
    }
}
