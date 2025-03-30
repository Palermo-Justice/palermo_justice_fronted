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

class RolesScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("comic-ui.json"))

        createUI()
    }

    private fun createUI() {
        // main table for the entire screen
        val mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        // header table
        val headerTable = Table()
        val titleLabel = Label("GAME ROLES", skin, "title")
        titleLabel.setAlignment(Align.center)

        // back button
        val backButton = TextButton("BACK", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        headerTable.add(backButton).padRight(20f).align(Align.left)
        headerTable.add(titleLabel).expandX().align(Align.center)

        // scrollable roles container
        val rolesTable = Table()
        val scrollPane = ScrollPane(rolesTable, skin)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)

        // add roles to the roles table
        addRole(rolesTable, "Mafioso", "A member of the mafia. Works with other mafiosi to eliminate citizens each night. Wins when mafia outnumbers citizens.", "mafioso.jpg")
        addRole(rolesTable, "Cittadino", "A regular citizen trying to survive. Votes during the day to eliminate suspected mafiosi. Wins when all mafiosi are eliminated.", "citizen.jpg")
        addRole(rolesTable, "Detective", "A citizen with the ability to investigate one player each night to determine if they are a mafioso. Wins with the citizens.", "detective.jpg")
        addRole(rolesTable, "Medico", "A citizen with the ability to protect one player each night from being eliminated. Cannot protect the same player in consecutive nights. Wins with the citizens.", "doctor.jpg")
        addRole(rolesTable, "Capo Mafia", "Leader of the mafia. If the mafia cannot reach a decision on who to eliminate, the Capo Mafia's choice prevails. Wins with the mafia.", "godfather.jpg")

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(20f).row()
        mainTable.add(scrollPane).expand().fill().pad(20f).row()

        // style adjustment
        backButton.pad(10f)
    }

    private fun addRole(table: Table, roleName: String, roleDescription: String, imagePath: String) {
        val roleTable = Table()
        roleTable.pad(10f)
        roleTable.background = skin.getDrawable("button")

        // Role image
        try {
            val fileHandle = Gdx.files.internal(imagePath)
            if (fileHandle.exists()) {
                val roleTexture = Texture(fileHandle)
                val roleImage = Image(roleTexture)
                roleTable.add(roleImage).size(100f, 100f).padRight(20f)
            } else {
                val placeholderLabel = Label("No Image", skin)
                roleTable.add(placeholderLabel).size(100f, 100f).padRight(20f)
            }
        } catch (e: Exception) {
            Gdx.app.error("ERROR", "Exception loading role image: $imagePath", e)
            val placeholderLabel = Label("No Image", skin)
            roleTable.add(placeholderLabel).size(100f, 100f).padRight(20f)
        }

        // Role information
        val infoTable = Table()
        val nameLabel = Label(roleName, skin, "title")
        val descriptionLabel = Label(roleDescription, skin)
        descriptionLabel.setWrap(true)

        infoTable.add(nameLabel).fillX().padBottom(10f).row()
        infoTable.add(descriptionLabel).fillX().expandX()

        roleTable.add(infoTable).expandX().fillX()

        // Add the role entry to the main roles table with padding
        table.add(roleTable).expandX().fillX().padBottom(20f).row()
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
