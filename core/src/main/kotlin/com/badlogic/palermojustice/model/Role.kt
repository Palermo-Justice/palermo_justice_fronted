package com.badlogic.palermojustice.model

abstract class Role(val name: String) {
    abstract val description: String
    open fun performAction(players: List<Player>, target: Player): String? {
        return null
    }
}

// Specific roles

//Mafia
class Mafioso : Role("Mafioso") {
    override val description = "Select a person to kill"
    override fun performAction(players: List<Player>, target: Player): String {
        if (target.isProtected) {
             return ("${target.name} was protected!")
        } else {
            target.isAlive = false
             return ("${target.name} was killed!")
        }
    }
}
//Detective
class Ispettore : Role("Ispettore") {
    override val description = "Select a person to inspect"
    override fun performAction(players: List<Player>, target: Player): String {
        return "${target.name} is a ${target.role?.name}"
    }
}

//Protector
class Sgarrista : Role("Sgarrista") {
    override val description = "Select a person to protect"
    override fun performAction(players: List<Player>, target: Player): String? {
        target.isProtected = true
        println("${target.name} is now protected!")
        return null
    }
}

//Villager
class Paesano : Role("Paesano") {
    override val description = "No action required"
    override fun performAction(players: List<Player>, target: Player): String? { return null }
}
