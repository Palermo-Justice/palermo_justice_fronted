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
        val titleLabel = Label("GAME ROLES", skin, "title")
        titleLabel.setAlignment(Align.center)

        // back button
        val backButton = TextButton("BACK", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Main.instance.setScreen(HomeScreen())
            }
        })

        headerTable.add(titleLabel).expandX().align(Align.center)

        // scrollable roles container
        val rolesTable = Table()
        val scrollPane = ScrollPane(rolesTable, skin)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)

        // add roles to the roles table
        addRole(rolesTable, "Mafioso", "A member of the mafia. Works with other mafiosi to eliminate citizens each night. Wins when mafia outnumbers citizens.", "godfather.jpg")
        addRole(rolesTable, "Paesano", "A regular player with no special abilities (from the italian slang it means \"Villager\n" + "or citizen of a country city\")", "citizen.jpg")
        addRole(rolesTable, "Ispettore", "A citizen with the ability to investigate one player each night to determine if they are a mafioso. Wins with the citizens.", "detective.jpg")
        addRole(rolesTable, "Sgarrista", "A citizen with the ability to protect one player each night from being eliminated. Cannot protect the same player in consecutive nights. Wins with the citizens.", "nnnn.jpg")
        addRole(rolesTable, "Il Prete", "A citizen with the ability to protect one player each night from being eliminated. Cannot protect the same player in consecutive nights. Wins with the citizens.", "doctor.jpg")

        // put all elements in the main table
        mainTable.add(headerTable).fillX().padTop(10f).padBottom(10f).row()
        mainTable.add(scrollPane).expand().fill().padBottom(50f).padLeft(5f).padRight(5f).row()
        mainTable.add(backButton).size(450f, 150f).padBottom(100f).center().row()
    }

    private fun addRole(table: Table, roleName: String, roleDescription: String, imagePath: String) {
        val roleTable = Table()
        roleTable.pad(10f)
        roleTable.background = skin.getDrawable("roles_box")

        // Role image
        try {
            val fileHandle = Gdx.files.internal(imagePath)
            if (fileHandle.exists()) {
                val roleTexture = Texture(fileHandle)
                val roleImage = Image(roleTexture)
                roleTable.add(roleImage).size(100f, 100f).padRight(20f)
            } else {
                val placeholderLabel = Label("", skin)
                roleTable.add(placeholderLabel).size(100f, 100f).padRight(10f)
            }
        } catch (e: Exception) {
            Gdx.app.error("ERROR", "Exception loading role image: $imagePath", e)
            val placeholderLabel = Label("No Image", skin)
            roleTable.add(placeholderLabel).size(100f, 100f).padRight(20f)
        }

        // Role information
        val infoTable = Table()
        val nameLabel = Label(roleName, skin, "title")
        nameLabel.style.font.data.setScale(1.3f)
        val descriptionLabel = Label(roleDescription, skin)
        descriptionLabel.style.font.data.setScale(2f)
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
