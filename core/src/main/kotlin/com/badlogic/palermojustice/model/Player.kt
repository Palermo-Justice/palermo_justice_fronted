package com.mygame.model.com.badlogic.palermojustice

enum class Role { PAESANO, MAFIOSO, ISPETTORE, SGARRISTA, PRETE, PENTITO, TRUFFATORE }

data class Player(
    val id: String,
    val name: String,
    var role: Role,
    var isAlive: Boolean = true,
    var isProtected: Boolean = false
)
