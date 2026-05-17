package io.github.sirmustfailalot.hatching

import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import io.github.sirmustfailalot.Announcement
import io.github.sirmustfailalot.Discord
import io.github.sirmustfailalot.ProjectAsh
import java.util.Locale
import kotlin.String
import kotlin.text.contains
import org.slf4j.LoggerFactory

object HatchAnnounce {
    private val logger = LoggerFactory.getLogger("project-ash")
    fun onHatch(context: HatchEggEvent) {
        val playerName = context.player.name?.string?:""
        val species = context.egg.species.toString()

        val formVariation: String? = context.egg.form
        logger.debug("Hatch Event Triggered Species: {} form: {}", species, formVariation)

        val shiny = context.egg.shiny!!
        val ivs = context.egg.ivs!!
        val hp = ivs[Stats.HP] ?: 0
        val atk = ivs[Stats.ATTACK] ?: 0
        val def = ivs[Stats.DEFENCE] ?: 0
        val spatk = ivs[Stats.SPECIAL_ATTACK] ?: 0
        val spdef = ivs[Stats.SPECIAL_DEFENCE] ?: 0
        val speed = ivs[Stats.SPEED] ?: 0

        val ivList = listOf(hp, atk, def, spatk, spdef, speed)
        val perfectCount = ivList.count { it == 31 }

        // Label Creation
        var labels = listOf("")
        if (perfectCount == 6 && shiny) {
            labels = listOf("shiny", "perfect")
        } else if (perfectCount == 6) {
            labels = listOf("perfect")
        } else if (shiny) {
            labels = listOf("shiny")
        }

        Announcement.hatched(server = ProjectAsh.server, hatchType = labels, species = species, playerName = playerName)
        Discord.announcement(eventType="Hatch", server=ProjectAsh.server, playerName=playerName, spawnType=labels, species=species, speciesPlusForm=species)
    }
}