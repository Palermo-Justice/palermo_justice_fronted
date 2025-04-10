package com.badlogic.palermojustice.model
import com.mygame.model.com.badlogic.palermojustice.Player

abstract class Role(val name: String) {
    abstract fun performAction(players: List<Player>, target: Player)
}

// Specific roles

//Mafia
class Mafioso : Role("Mafioso") {
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
    override fun performAction(players: List<Player>, target: Player) {
        println("${target.name} is a ${target.role.name}")
    }
}

//Protector
class Sgarrista : Role("Sgarrista") {
    override fun performAction(players: List<Player>, target: Player) {
        target.isProtected = true
    }
}

//Villager
class Paesano : Role("Paesano") {
    override fun performAction(players: List<Player>, target: Player) {}
}
