package com.mygame.model.com.badlogic.palermojustice

import com.badlogic.palermojustice.model.Role

data class Player(
    val id: String,
    val name: String,
    var role: Role,
    var isAlive: Boolean = true,
    var isProtected: Boolean = false
)
