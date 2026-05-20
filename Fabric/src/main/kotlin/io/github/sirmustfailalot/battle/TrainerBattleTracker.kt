package io.github.sirmustfailalot.battle

import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import io.github.sirmustfailalot.ProjectAsh
import java.util.UUID

object TrainerBattleTracker {

    /**
     * Helper to safely locate a trainer's display name from the server worlds via UUID.
     */
    private fun getTrainerNameByUuid(uuid: UUID): String {
        val server = ProjectAsh.server ?: return "Trainer"
        for (level in server.allLevels) {
            val entity = level.getEntity(uuid)
            if (entity != null) {
                return entity.customName?.string ?: entity.displayName!!.string
            }
        }
        return "Trainer"
    }

    fun onBattleStarted(event: BattleStartedEvent.Post) {
        val battle = event.battle
        val server = ProjectAsh.server ?: return

        // Separate players and trainers safely using Cobblemon ActorTypes
        val players = battle.actors
            .filter { it.type == ActorType.PLAYER }
            .mapNotNull { server.playerList.getPlayer(it.uuid) }

        if (players.isEmpty()) return

        val startMessage: Component

        // DYNAMIC LAYOUT CHECK 1: Check if this match format is classified as PvP
        if (battle.isPvP) {
            val side1Players = battle.side1.actors.filter { it.type == ActorType.PLAYER }.mapNotNull { server.playerList.getPlayer(it.uuid) }.joinToString(", ") { it.scoreboardName }
            val side2Players = battle.side2.actors.filter { it.type == ActorType.PLAYER }.mapNotNull { server.playerList.getPlayer(it.uuid) }.joinToString(", ") { it.scoreboardName }

            startMessage = Component.literal("⚔️ §bPvP Battle Started: $side1Players §7vs $side2Players")
        } else {
            // Otherwise, treat it as a standard Wild Pokemon or Trainer fight
            val trainers = battle.actors.filter { it.type == ActorType.NPC }
            if (trainers.isEmpty()) return // Skip if it's just a raw wild pokemon encounter

            val playerNames = players.joinToString(", ") { it.scoreboardName }
            val trainerNames = trainers.map { getTrainerNameByUuid(it.uuid) }.joinToString(", ")

            startMessage = Component.literal("§eBattle Started: $playerNames §7vs $trainerNames")
        }

        server.playerList.players.forEach { p ->
            p.sendSystemMessage(startMessage)
        }
    }

    /**
     * Triggers when the battle concludes.
     */
    fun onBattleCompleted(event: BattleVictoryEvent) {
        val battle = event.battle
        val server = ProjectAsh.server ?: return

        // DYNAMIC LAYOUT CHECK 2: Determine if we should process this as a PvP or PvN/Trainer battle
        val isPvP = battle.isPvP
        val trainers = battle.actors.filter { it.type == ActorType.NPC }

        // If it's not a PvP match AND there are no NPC trainers, skip tracking (it was a wild pokemon encounter)
        if (!isPvP && trainers.isEmpty()) return

        val totalActors = battle.actors.filter { it.type == ActorType.PLAYER || it.type == ActorType.NPC }
        val winningActors = event.winners
        val losingActors = totalActors.filter { !winningActors.contains(it) }

        fun resolveActorName(actor: com.cobblemon.mod.common.api.battles.model.actor.BattleActor): String {
            return if (actor.type == ActorType.PLAYER) {
                server.playerList.getPlayer(actor.uuid)?.scoreboardName ?: "Player"
            } else {
                getTrainerNameByUuid(actor.uuid)
            }
        }

        // 1. Build local in-game chat summary
        val winnersString = winningActors.joinToString(", ") { "§a" + resolveActorName(it) }
        val losersString = losingActors.joinToString(", ") { "§c" + resolveActorName(it) }

        val inGameSummary = Component.literal("§eBattle Finished: $winnersString §7vs $losersString")

        server.playerList.players.forEach { p ->
            p.sendSystemMessage(inGameSummary)
        }

        // Native fog-of-war collection: Gather opponent pokemon that the players' teams faced directly
        val playerRevealedPokemonUuids = totalActors
            .filter { it.type == ActorType.PLAYER }
            .flatMap { playerActor -> playerActor.pokemonList }
            .flatMap { playerPokemon -> playerPokemon.facedOpponents } // Derived straight from native class turn history loop
            .map { seenPokemon -> seenPokemon.uuid }
            .toSet()

        // 2. Build the detailed team summary arrays for Discord
        val discordSummaries = totalActors.map { actor ->
            val name = resolveActorName(actor)
            val isWinner = winningActors.contains(actor)

            // FILTER RULE: Only hide unrevealed bench slots for NPC trainers (ActorType.NPC)
            // If it's a real player (even in a PvP match), show their full roster transparently
            val targetPokemonList = if (actor.type == ActorType.NPC) {
                actor.pokemonList.filter { npcPokemon -> playerRevealedPokemonUuids.contains(npcPokemon.uuid) }
            } else {
                actor.pokemonList
            }

            val pokemonStatusList = targetPokemonList.map { battlePokemon ->
                val pokemonInstance = battlePokemon.originalPokemon
                io.github.sirmustfailalot.DiscordPokemonStatus(
                    name = pokemonInstance.species.name,
                    level = pokemonInstance.level,
                    isShiny = pokemonInstance.shiny,
                    isFainted = battlePokemon.health <= 0 || pokemonInstance.currentHealth <= 0
                )
            }.toMutableList()

            // Append fog-of-war entries only if it's an NPC trainer box slot
            if (actor.type == ActorType.NPC) {
                val unseenCount = actor.pokemonList.size - targetPokemonList.size
                if (unseenCount > 0) {
                    repeat(unseenCount) {
                        pokemonStatusList.add(
                            io.github.sirmustfailalot.DiscordPokemonStatus(
                                name = "???",
                                level = 0,
                                isShiny = false,
                                isFainted = false
                            )
                        )
                    }
                }
            }

            io.github.sirmustfailalot.DiscordParticipantSummary(
                displayName = name,
                isWinner = isWinner,
                pokemonList = pokemonStatusList
            )
        }

        // 3. Fire to the Discord Webhook Engine
        io.github.sirmustfailalot.Discord.battleFinished(server, discordSummaries)
    }
}