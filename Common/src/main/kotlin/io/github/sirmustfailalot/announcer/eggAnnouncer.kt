package io.github.sirmustfailalot.projectash.announcer

// ProjectAsh Classes
import io.github.sirmustfailalot.projectash.pipeline.PokeStream

// General Logger and Other Classes
import com.google.gson.Gson
import io.github.sirmustfailalot.projectash.subscribers.EggHatching
import org.slf4j.LoggerFactory

object eggAnnouncer {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    private val gson = Gson()

    fun announceHatch(
        pokeGlance: PokeStream.PokemonLifespan,
        hatchedBy: String
    ) {
        val announceDetails = pokeGlance.evaluationResult!!
        val inGameText = when (pokeGlance.spawnSource) {
            "Egg" -> "$hatchedBy has hatched a ${pokeGlance.speciesWithForm}!"
            else -> return
        }
        logger.info("EGG announcer: ${inGameText}")
        val fields = listOf(
            EmbedField("Hatch Traits", pokeGlance.evaluationResult!!.discordCriteria.serverLabels.joinToString()),
            EmbedField("Hatched By", hatchedBy),
        )

        DeliveryAnnouncer.executeBroadcast(
            iconPrefix = "🐣",
            eventState = "Hatch",
            pokeGlance = pokeGlance,
            announceDetails = announceDetails,
            customDiscordFields = fields,
            customThumbnail = pokeGlance.sprite,
            inGameText = inGameText
        )
    }
}