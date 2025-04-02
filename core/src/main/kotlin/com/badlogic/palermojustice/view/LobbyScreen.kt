package com.badlogic.palermojustice.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.graphics.g2d.TextureRegion

class LobbyScreen : Screen {
    private val stage = Stage(ScreenViewport())

    private lateinit var font: BitmapFont
    private lateinit var labelStyle: Label.LabelStyle
    private lateinit var buttonStyle: TextButton.TextButtonStyle
    private lateinit var textFieldStyle: TextField.TextFieldStyle
    private lateinit var playersTextFields: MutableList<TextField>

    override fun show() {
        Gdx.input.inputProcessor = stage

        font = BitmapFont().apply { data.setScale(3f) }

        labelStyle = Label.LabelStyle().apply {
            font = this@LobbyScreen.font
            fontColor = Color.WHITE
        }

        buttonStyle = TextButton.TextButtonStyle().apply {
            font = this@LobbyScreen.font
            fontColor = Color.WHITE
        }

        textFieldStyle = TextField.TextFieldStyle().apply {
            font = this@LobbyScreen.font
            fontColor = Color.WHITE
        }

        val gameNameLabel = Label("Palermo Justice", labelStyle)

        val gearTexture = Texture(Gdx.files.internal("gear.jpg"))
        val gearDrawable = TextureRegionDrawable(TextureRegion(gearTexture))
        val gearButton = ImageButton(gearDrawable)
        gearButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                println("Gear button clicked!")
            }
        })

        // Top bar for game name & settings
        val topTable = Table()
        topTable.add(gameNameLabel).left().expandX()
        topTable.add(gearButton).right().width(80f).height(80f)


        // room code data
        val codeLabel = Label("Code:", labelStyle)
        val codeValueLabel = Label("5AGHC", labelStyle) // dummy code value

        val codeTable = Table()
        codeTable.add(codeLabel).padRight(30f)
        codeTable.add(codeValueLabel)


        // player data
        playersTextFields = mutableListOf()
        val numberOfPlayers = 7 // to support dynamic number in future
        val dummyNames = listOf("Bari", "Niklas", "Panta", "Gaute", "Moritz", "Markus", "Justin") // dummy player names for now
        val playerNameStyle = TextField.TextFieldStyle().apply {
            font = this@LobbyScreen.font
            fontColor = Color.BLACK
            font.data.setScale(3f)

            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                setColor(Color.WHITE)
                fill()
            }
            val blackDrawable = TextureRegionDrawable(TextureRegion(Texture(pixmap)))
            pixmap.dispose()

            background = blackDrawable
            background.setLeftWidth(10f)
            background.setRightWidth(10f)
            background.setTopHeight(10f)
            background.setBottomHeight(10f)
        }
        // Player fields vertically
        val playersTable = Table()
        playersTable.defaults().pad(10f).width(450f).height(100f)
        for (i in 0 until numberOfPlayers) {
            val nameForPlayer = if (i < dummyNames.size) dummyNames[i] else "Player ${i + 1}"
            val textField = TextField(nameForPlayer, playerNameStyle)
            playersTextFields.add(textField)
            playersTable.add(textField).row()
        }

        // TODO: replace with rolelist page after implmeneted
        val rolesButton = TextButton("Roles", buttonStyle)
        rolesButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                println("Roles button clicked!")
            }
        })

        // TODO: replace with start game page (role assignment page?)
        val startButton = TextButton("Start", buttonStyle)
        startButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                println("Start button clicked!")
            }
        })
        val bottomTable = Table()
        bottomTable.defaults().width(250f).height(100f).pad(20f)
        bottomTable.add(rolesButton)
        bottomTable.add(startButton)

        val rootTable = Table()
        rootTable.setFillParent(true)
        rootTable.top().pad(20f)
        rootTable.add(topTable).fillX().expandX().row()
        rootTable.add(codeTable).pad(40f).row()
        rootTable.add(playersTable).padBottom(40f).row()
        rootTable.add(bottomTable).center()

        stage.addActor(rootTable)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
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
        font.dispose()
    }
}
