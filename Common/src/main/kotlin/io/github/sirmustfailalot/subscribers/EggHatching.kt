package io.github.sirmustfailalot.projectash.subscribers

// Cobblemon Classes
import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import io.github.sirmustfailalot.projectash.announcer.eggAnnouncer
import io.github.sirmustfailalot.projectash.pipeline.DiscordCriteria
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.pipeline.RuleEvaluationResult
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers.server

// General Logger and Other Classes
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull

object EggHatching {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    fun onHatch(
        event: HatchEggEvent.Post
    ) {
        try {
            val playerName = event.player.name.string
            val pokeGlance = PokeStream.pokeGlance(event.pokemon, "Egg")
            val freshEvaluation = RuleEvaluationResult()
            if (pokeGlance.isShiny || pokeGlance.isPerfectIV) {
                freshEvaluation.discordCriteria.isServerMessage = true

                if (pokeGlance.isShiny) {
                    freshEvaluation.discordCriteria.serverLabels.add("Shiny")
                    freshEvaluation.discordCriteria.serverRules.add("Shiny Rule")
                }
                if (pokeGlance.isPerfectIV) {
                    freshEvaluation.discordCriteria.serverLabels.add("Perfect IV")
                    freshEvaluation.discordCriteria.serverRules.add("Perfect IV Rule")
                }
            }

            pokeGlance.evaluationResult = freshEvaluation

            eggAnnouncer.announceHatch(
                pokeGlance = pokeGlance,
                hatchedBy = playerName
            )

        } catch (e: Exception) {
            logger.error("Project Ash: Critical error occurred during egg hatch processing!", e)
        }
    }
}