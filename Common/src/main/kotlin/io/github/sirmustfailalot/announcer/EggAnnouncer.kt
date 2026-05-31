package io.github.sirmustfailalot.projectash.announcer

// ProjectAsh Classes
import io.github.sirmustfailalot.projectash.pipeline.PokeStream

// General Logger and Other Classes
import com.google.gson.Gson
import org.slf4j.LoggerFactory

object EggAnnouncer {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    private val gson = Gson()

    fun announceHatch(
        hatchGlance: PokeStream.PokemonLifespan,
        hatchedBy: String
    ) {
        val announceDetails = hatchGlance.evaluationResult!!
        val inGameText = when (hatchGlance.spawnSource) {
            "Egg" -> "$hatchedBy has hatched a ${hatchGlance.speciesWithForm}!"
            else -> return
        }
        val fields = listOf(
            EmbedField("Hatch Traits", hatchGlance.evaluationResult!!.discordCriteria.serverLabels.joinToString()),
            EmbedField("Hatched By", hatchedBy),
        )

        DeliveryAnnouncer.executeBroadcast(
            iconPrefix = "🐣",
            eventState = "Hatch",
            pokeGlance = hatchGlance,
            announceDetails = announceDetails,
            customDiscordFields = fields,
            customThumbnail = hatchGlance.sprite,
            inGameText = inGameText
        )
    }
}