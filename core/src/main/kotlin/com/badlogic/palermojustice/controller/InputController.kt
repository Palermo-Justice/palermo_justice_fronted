package com.badlogic.palermojustice.controller

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2

class InputController : InputProcessor {
    private val gameController = GameController.getInstance()

    fun handleTouch(x: Int, y: Int) {
        // convert actions from touch and send to controller
    }

    fun handleKeyboard() {
        // Manage keyboard input
    }

    fun processInput() {
        // Manage other input
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

    // Implement input processor
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
}
