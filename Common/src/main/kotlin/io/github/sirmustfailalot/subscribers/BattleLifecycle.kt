package io.github.sirmustfailalot.projectash.subscribers

// Project Ash Classes
import io.github.sirmustfailalot.projectash.pipeline.BattleStream
import io.github.sirmustfailalot.projectash.announcer.BattleAnnouncer
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers.server

// Cobblemon Classes
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor

// Other Classes
import java.util.UUID

object BattleLifecycle {
    private fun getTrainerNameByUuid(uuid: UUID): String {
        val server = server ?: return "Trainer"
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
        val server = server ?: return

        if (battle.isPvW) return

        val players = battle.actors
            .filter { it.type == ActorType.PLAYER }
            .mapNotNull { server.playerList.getPlayer(it.uuid) }
        if (players.isEmpty()) return

        val side1Players = battle.side1.actors.filter { it.type == ActorType.PLAYER }.mapNotNull { server.playerList.getPlayer(it.uuid) }.joinToString(", ") { it.scoreboardName }
        val side2Players = battle.side2.actors.filter { it.type == ActorType.PLAYER }.mapNotNull { server.playerList.getPlayer(it.uuid) }.joinToString(", ") { it.scoreboardName }

        val opponentNames = if (battle.isPvP) side2Players else battle.actors.filter { it.type == ActorType.NPC }.map { getTrainerNameByUuid(it.uuid) }.joinToString(", ")
        val playerNames = if (battle.isPvP) side1Players else players.joinToString(", ") { it.scoreboardName }

        val battleGlance = BattleStream.BattleGlance(
            isPvP = battle.isPvP,
            playerNames = playerNames,
            opponentNames = opponentNames
        )

        BattleAnnouncer.announceBattleStart(server, battleGlance)
    }

    fun onBattleCompleted(event: BattleVictoryEvent) {
        val battle = event.battle
        val server = server ?: return
        if (battle.isPvW) return

        val isPvP = battle.isPvP
        val trainers = battle.actors.filter { it.type == ActorType.NPC }
        if (!isPvP && trainers.isEmpty()) return

        val totalActors = battle.actors.filter { it.type == ActorType.PLAYER || it.type == ActorType.NPC }
        val winningActors = event.winners

        fun resolveActorName(actor: BattleActor): String {
            return if (actor.type == ActorType.PLAYER) {
                server.playerList.getPlayer(actor.uuid)?.scoreboardName ?: "Player"
            } else {
                getTrainerNameByUuid(actor.uuid)
            }
        }

        val playerRevealedPokemonUuids = totalActors
            .filter { it.type == ActorType.PLAYER }
            .flatMap { playerActor -> playerActor.pokemonList }
            .flatMap { playerPokemon -> playerPokemon.facedOpponents }
            .map { seenPokemon -> seenPokemon.uuid }
            .toSet()

        val participantSnapshots = totalActors.map { actor ->
            val name = resolveActorName(actor)
            val isWinner = winningActors.contains(actor)

            val targetPokemonList = if (actor.type == ActorType.NPC) {
                actor.pokemonList.filter { npcPokemon -> playerRevealedPokemonUuids.contains(npcPokemon.uuid) }
            } else {
                actor.pokemonList
            }

            val pokemonStatusList = targetPokemonList.map { battlePokemon ->
                val pokemonInstance = battlePokemon.originalPokemon
                BattleStream.BattlePokemonSnapshot(
                    name = pokemonInstance.species.name,
                    level = pokemonInstance.level,
                    isShiny = pokemonInstance.shiny,
                    isFainted = battlePokemon.health <= 0 || pokemonInstance.currentHealth <= 0
                )
            }.toMutableList()

            if (actor.type == ActorType.NPC) {
                val unseenCount = actor.pokemonList.size - targetPokemonList.size
                if (unseenCount > 0) {
                    repeat(unseenCount) {
                        pokemonStatusList.add(
                            BattleStream.BattlePokemonSnapshot(name = "???", level = 0, isShiny = false, isFainted = false)
                        )
                    }
                }
            }

            BattleStream.BattleParticipantSnapshot(
                displayName = name,
                isWinner = isWinner,
                teams = pokemonStatusList
            )
        }

        val summaryGlance = BattleStream.BattleSummaryGlance(
            participants = participantSnapshots
        )

        BattleAnnouncer.announceBattleVictory(server, summaryGlance)
    }
}