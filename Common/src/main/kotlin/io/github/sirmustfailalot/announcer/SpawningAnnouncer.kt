package io.github.sirmustfailalot.projectash.announcer

import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.pipeline.RuleEvaluationResult

import org.slf4j.LoggerFactory

object SpawningAnnouncer {
    private val logger = LoggerFactory.getLogger("ProjectAsh")


    fun announceSpawn(
        pokeGlance: PokeStream.PokemonLifespan,
        announceDetails: RuleEvaluationResult
    ) {
        val nearMessage = if (pokeGlance.spawnDimension == "Overworld") {
            "near ${pokeGlance.spawnClosestPlayer} at ${pokeGlance.spawnPos}"
        } else {
            "near ${pokeGlance.spawnClosestPlayer} at ${pokeGlance.spawnPos} (${pokeGlance.spawnDimension})"
        }

        val inGameText = when (pokeGlance.spawnSource) {
            "Unknown" -> "${pokeGlance.speciesWithForm} has somehow spawned $nearMessage"
            "Known" -> "${pokeGlance.speciesWithForm} spawned $nearMessage"
            else -> "Different Spawn"
        }

        val fields = listOf(
            EmbedField("Dimension", pokeGlance.spawnDimension),
            EmbedField("Closest Player", pokeGlance.spawnClosestPlayer),
            EmbedField("Position", "`${pokeGlance.spawnPos}`")
        )

        DeliveryAnnouncer.executeBroadcast(
            iconPrefix = if (announceDetails.discordCriteria.serverLabels.contains("Shiny")) "✨ " else "",
            eventState = "Spawn",
            pokeGlance = pokeGlance,
            announceDetails = announceDetails,
            customDiscordFields = fields,
            customThumbnail = pokeGlance.sprite,
            inGameText = inGameText
        )
    }

    fun announceCapture(caughtBy: String, pokeGlance: PokeStream.PokemonLifespan, announceDetails: RuleEvaluationResult) {
        DeliveryAnnouncer.executeBroadcast(
            iconPrefix = "✅ ",
            eventState = "Captured",
            pokeGlance = pokeGlance,
            announceDetails = announceDetails,
            customDiscordFields = listOf(EmbedField("Caught By", caughtBy)),
            customThumbnail = pokeGlance.sprite,
            inGameText = "${pokeGlance.speciesWithForm} was caught by $caughtBy!"
        )
    }

    fun announceFainted(pokeGlance: PokeStream.PokemonLifespan, announceDetails: RuleEvaluationResult) {
        DeliveryAnnouncer.executeBroadcast(
            iconPrefix = "❌ ",
            eventState = "Fainted",
            pokeGlance = pokeGlance,
            announceDetails = announceDetails,
            customThumbnail = "https://s-media-cache-ak0.pinimg.com/600x315/b1/20/08/b120087f3a904bda147251beaedf5755.jpg",
            inGameText = "${pokeGlance.speciesWithForm} Fainted...... Well..... Back to it then!"
        )
    }

    fun announceRemoved(removalReason: String, pokeGlance: PokeStream.PokemonLifespan, announceDetails: RuleEvaluationResult) {
        DeliveryAnnouncer.executeBroadcast(
            iconPrefix = "❌ ",
            eventState = "Despawned",
            pokeGlance = pokeGlance,
            announceDetails = announceDetails,
            customThumbnail = "https://i.pinimg.com/originals/a9/48/e0/a948e0a1af81e162fe766faeeba3bc51.jpg",
            inGameText = "${pokeGlance.speciesWithForm} Despawned"
        )
    }
}