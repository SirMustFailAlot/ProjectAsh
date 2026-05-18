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
import kotlin.jvm.optionals.getOrNull
import kotlin.text.contains

object HatchAnnounce {
    private val logger = LoggerFactory.getLogger("project-ash")
    fun onHatch(context: HatchEggEvent.Post) {
        val cachedProfile = ProjectAsh.server?.profileCache?.get(context.player.uuid)?.getOrNull()
        val playerName = cachedProfile?.name
            ?: ProjectAsh.server?.playerList?.getPlayer(context.player.uuid)?.name?.string
            ?: "Unknown Player"
        val species = context.pokemon.species.translatedName.string

        val formVariation: String? = context.pokemon.form.labels
            .asSequence()
            .map { it.toString() }
            .firstOrNull { it.contains("_form", ignoreCase = true) }
            ?.substringBefore("_form")
            ?.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val speciesPlusForm = if (formVariation.isNullOrBlank()) {
            species
        } else {
            "$species ($formVariation)"
        }

        val shiny = context.pokemon.shiny
        val ivs = context.pokemon.ivs!!
        val hp = ivs[Stats.HP] ?: 0
        val atk = ivs[Stats.ATTACK] ?: 0
        val def = ivs[Stats.DEFENCE] ?: 0
        val spatk = ivs[Stats.SPECIAL_ATTACK] ?: 0
        val spdef = ivs[Stats.SPECIAL_DEFENCE] ?: 0
        val speed = ivs[Stats.SPEED] ?: 0

        val ivList = listOf(hp, atk, def, spatk, spdef, speed)
        val perfectCount = ivList.count { it == 31 }


        var labels = listOf("")
        if (perfectCount == 6 && shiny) {
            labels = listOf("perfect", "shiny")
        } else if (perfectCount == 6) {
            labels = listOf("perfect")
        } else if (shiny) {
            labels = listOf("shiny")
        }

        Announcement.hatched(server = ProjectAsh.server, hatchType = labels, species = speciesPlusForm, playerName = playerName)
        Discord.announcement(eventType="Hatched", server=ProjectAsh.server, playerName=playerName, spawnType=labels, species=species, speciesPlusForm=speciesPlusForm)
    }
}