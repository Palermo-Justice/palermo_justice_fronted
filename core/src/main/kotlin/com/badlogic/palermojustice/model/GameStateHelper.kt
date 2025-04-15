package com.badlogic.palermojustice.model

import com.badlogic.palermojustice.controller.GameController

object GameStateHelper {
    // Game flow control properties
    val roleSequence = listOf("Ispettore", "Mafioso", "Sgarrista")
    var currentRoleIndex = 0
    var currentRolePlayers: MutableList<Player> = mutableListOf()
    var currentActingPlayerIndex = 0

    /**
     * Assigns roles to players. Now uses GameModel to access the player list.
     */
    fun assignRoles() {
        // Get player list from GameModel
        val gameController = GameController.getInstance()

        // Let the model handle role assignment
        gameController.model.assignRoles()
    }

    /**
     * Gets the next role that should act during the night phase.
     * @return The next role in the sequence or null if we've completed all roles
     */
    fun getNextNightRole(): String? {
        if (currentRoleIndex >= roleSequence.size) {
            return null // No more roles to process
        }

        return roleSequence[currentRoleIndex++]
    }

    /**
     * Prepares players of a specific role to act during the night.
     * @param roleName The name of the role to prepare
     * @return True if there are players with this role, false otherwise
     */
    fun prepareRolePlayers(roleName: String): Boolean {
        val gameController = GameController.getInstance()

        // Convert role name to a Role instance
        val role = when (roleName) {
            "Mafioso" -> Mafioso()
            "Ispettore" -> Ispettore()
            "Sgarrista" -> Sgarrista()
            "Paesano" -> Paesano()
            else -> return false // Unknown role
        }

        // Get living players with this role
        // We need to match by name since we can't directly compare role instances
        currentRolePlayers = gameController.model.getPlayers()
            .filter {
                it.isAlive &&
                    it.role != null &&
                    it.role!!.name == role.name
            }
            .toMutableList()

        currentActingPlayerIndex = 0

        return currentRolePlayers.isNotEmpty()
    }

    /**
     * Resets the night sequence to start a new night.
     */
    fun resetNightSequence() {
        currentRoleIndex = 0
        currentRolePlayers.clear()
        currentActingPlayerIndex = 0
    }

    /**
     * Process a night action for a player with a specific role.
     * @param actingPlayer The player performing the action
     * @param targetPlayer The target of the action
     * @return True if the action was processed successfully
     */
    fun processNightAction(actingPlayer: Player, targetPlayer: Player): String? {
        if (actingPlayer.role == null) {
            return ""
        }

        // Let the role handle the action
        return actingPlayer.role!!.performAction(
            GameController.getInstance().model.getPlayers(),
            targetPlayer
        )
    }
}
