package io.github.sirmustfailalot.projectash.subscribers

// Cobblemon Classes
import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import io.github.sirmustfailalot.projectash.announcer.EggAnnouncer
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.pipeline.RuleEngine
import io.github.sirmustfailalot.projectash.pipeline.RuleEvaluationResult

// General Logger and Other Classes
import org.slf4j.LoggerFactory

object EggHatching {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    fun onHatch(
        event: HatchEggEvent.Post
    ) {
        val playerName = event.player.name.string
        val hatchGlance = PokeStream.hatchGlance(event.pokemon, "Egg")
        hatchGlance.evaluationResult = RuleEngine.evaluateSpawn(hatchGlance)

        EggAnnouncer.announceHatch(
            hatchGlance = hatchGlance,
            hatchedBy = playerName
        )
    }
}