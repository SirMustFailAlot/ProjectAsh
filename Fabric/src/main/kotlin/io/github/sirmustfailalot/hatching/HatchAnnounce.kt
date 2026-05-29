package io.github.sirmustfailalot.hatching

import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon
import io.github.sirmustfailalot.Announcement
import io.github.sirmustfailalot.Discord
import io.github.sirmustfailalot.ProjectAsh
import io.github.sirmustfailalot.utility.PokemonUtility
import java.util.Locale
import kotlin.String
import kotlin.text.contains
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull
import kotlin.text.contains

object HatchAnnounce {
    private val logger = LoggerFactory.getLogger("project-ash")
//    fun onHatch(context: HatchEggEvent.Post) {
//        val cachedProfile = ProjectAsh.server?.profileCache?.get(context.player.uuid)?.getOrNull()
//        val playerName = cachedProfile?.name
//            ?: ProjectAsh.server?.playerList?.getPlayer(context.player.uuid)?.name?.string
//            ?: "Unknown Player"
//
//        val pokeGlance = PokemonUtility.quickGlance(pokemonEntity = context.pokemon.entity!!)
//
//        Announcement.hatched(server = ProjectAsh.server, hatchType = pokeGlance.hatchingLabels, species = pokeGlance.speciesWithForm, playerName = playerName)
//        Discord.announcement(eventType="Hatched", server=ProjectAsh.server, playerName=playerName, spawnType=pokeGlance.hatchingLabels, species=pokeGlance.species, speciesPlusForm=pokeGlance.speciesWithForm, thumbnailURL = pokeGlance.thumbnail)
//    }
}