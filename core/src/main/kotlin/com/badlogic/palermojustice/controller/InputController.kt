package com.badlogic.palermojustice.controller

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2

class InputController : InputProcessor {
    private val gameController = GameController.getInstance()

    fun handleTouch(x: Int, y: Int) {
        // Converti coordinate touch in azioni di gioco
        // e invia tramite gameController
    }

    fun handleKeyboard() {
        // Gestisci input da tastiera
    }

    fun processInput() {
        // Elabora input accumulato
    }

    override fun keyDown(keycode: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun keyUp(keycode: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun keyTyped(character: Char): Boolean {
        TODO("Not yet implemented")
    }

    // Implementa i metodi di InputProcessor
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        handleTouch(screenX, screenY)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        TODO("Not yet implemented")
    }

    // Altri metodi di InputProcessor...
}
