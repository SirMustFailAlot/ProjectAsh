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

    /**
     * Triggers when the battle begins.
     * Layout: [Project Ash] Battle Started: Player1, Player2 vs Trainer1, Trainer2
     */
    fun onBattleStarted(event: BattleStartedEvent.Post) {
        val battle = event.battle
        val server = io.github.sirmustfailalot.ProjectAsh.server ?: return

        val players = battle.actors
            .filter { it.type == ActorType.PLAYER }
            .mapNotNull { server.playerList.getPlayer(it.uuid) }

        if (players.isEmpty()) return

        val trainers = battle.actors.filter { it.type == ActorType.NPC }
        if (trainers.isEmpty()) return

        val playerNames = players.joinToString(", ") { it.scoreboardName }

        // FIX: Removed .distinct() so both trainers show up even if they share the same name/type
        val trainerNames = trainers.map { getTrainerNameByUuid(it.uuid) }.joinToString(", ")

        val startMessage = Component.literal("§6[Project Ash] §eBattle Started: §f$playerNames §7vs §b$trainerNames")
        ProjectAsh.server?.playerList?.players?.forEach { p ->
            p.sendSystemMessage(startMessage)
        }
    }

    /**
     * Triggers when the battle concludes.
     * Layout: [Project Ash] Battle Finished: Winners (Green) vs Losers (Red)
     */
    fun onBattleCompleted(event: BattleVictoryEvent) {
        val battle = event.battle
        val server = io.github.sirmustfailalot.ProjectAsh.server ?: return

        val trainers = battle.actors.filter { it.type == ActorType.NPC }
        if (trainers.isEmpty()) return

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

        // 1. Build a clean, single-line local in-game chat summary
        val winnersString = winningActors.joinToString(", ") { "§a" + resolveActorName(it) }
        val losersString = losingActors.joinToString(", ") { "§c" + resolveActorName(it) }

        val inGameSummary = Component.literal("§6[Project Ash] §eBattle Finished: $winnersString §7vs $losersString")

        // Broadcast the simple summary to the server players
        server.playerList.players.forEach { p ->
            p.sendSystemMessage(inGameSummary)
        }

        // 2. Build the detailed Discord summary payloads (keeping pokemon details here)
        val discordSummaries = totalActors.map { actor ->
            val name = resolveActorName(actor)
            val isWinner = winningActors.contains(actor)

            val pokemonStatusList = actor.pokemonList.map { battlePokemon ->
                val pokemonInstance = battlePokemon.originalPokemon
                io.github.sirmustfailalot.DiscordPokemonStatus(
                    name = pokemonInstance.species.name,
                    level = pokemonInstance.level,
                    isShiny = pokemonInstance.shiny,
                    isFainted = battlePokemon.health <= 0 || pokemonInstance.currentHealth <= 0
                )
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