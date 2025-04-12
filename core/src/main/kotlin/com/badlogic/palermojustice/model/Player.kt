package com.badlogic.palermojustice.model

data class Player(
    val id: String,
    val name: String,
    var role: Role,
    var isAlive: Boolean = true,
    var isProtected: Boolean = false
)
