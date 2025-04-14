package com.badlogic.palermojustice.model
import com.mygame.model.com.badlogic.palermojustice.Player

abstract class Role(val name: String) {
    abstract val description: String
    abstract fun performAction(players: List<Player>, target: Player)
}

// Specific roles

//Mafia
class Mafioso : Role("Mafioso") {
    override val description = "Select a person to kill"
    override fun performAction(players: List<Player>, target: Player) {
        if (target.isProtected) {
            println("${target.name} was protected!")
        } else {
            target.isAlive = false
            println("${target.name} was killed!")
        }
    }
}

//Detective
class Ispettore : Role("Ispettore") {
    override val description = "Select a person to inspect"
    override fun performAction(players: List<Player>, target: Player) {
        println("${target.name} is a ${target.role.name}")
    }
}

//Protector
class Sgarrista : Role("Sgarrista") {
    override val description = "Select a person to protect"
    override fun performAction(players: List<Player>, target: Player) {
        target.isProtected = true
    }
}

//Villager
class Paesano : Role("Paesano") {
    override val description = "No action required"
    override fun performAction(players: List<Player>, target: Player) {}
}
