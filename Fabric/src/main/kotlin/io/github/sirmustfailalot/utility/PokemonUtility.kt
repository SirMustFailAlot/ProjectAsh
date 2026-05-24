package io.github.sirmustfailalot.utility

import SpawnTracker.spawnResult
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokedex.Dexes
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

import com.cobblemon.mod.common.api.pokedex.PokedexManager
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import io.github.sirmustfailalot.Config
import io.github.sirmustfailalot.ProjectAsh
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import java.io.FileInputStream
import java.util.Locale
import kotlin.String
import kotlin.text.contains
import kotlin.text.lowercase


object PokemonUtility {
    data class PokemonStream(
        val species: String,
        val speciesWithForm: String,
        val speciesForm: String,
        val thumbnail: String,
        val shiny: Boolean,
        val perfectIV: Boolean,
        val pokemonLabel: String? = null,
        val hatchingLabels: List<String> = listOf(),
        val catchEmAllPlayers: List<String> = listOf()
    )

    fun quickGlance(
        pokemonEntity: PokemonEntity
    ): PokemonStream {
        val pokemon = pokemonEntity.pokemon
        val species = pokemonEntity.pokemon.species.translatedName.string

        val formVariation: String? = pokemon.form.labels
            .asSequence()
            .map { it.toString() }
            .firstOrNull { it.contains("_form", ignoreCase = true) }
            ?.substringBefore("_form")
            ?.replaceFirstChar { it.titlecase(Locale.ROOT) }

        var nonOriginalFormFound = false;

        if (!formVariation.isNullOrBlank()) {
            pokemon.features.forEach {
                if (it.name.trim().lowercase() == formVariation.trim().lowercase()) {
                    nonOriginalFormFound = true
                }
            }
        }
        var speciesWithForm = ""
        var thumbnailLinkRawName = species

        if (formVariation.isNullOrBlank() || !nonOriginalFormFound) {
            speciesWithForm = species
        } else {
            if (pokemon.species.translatedName.string == "Tauros") {
                when (pokemon.form.name) {
                    "Paldea-Combat" -> {
                        speciesWithForm = "Paldean $species (Combat)"
                        thumbnailLinkRawName = "${species}_paldea_combat_breed"
                    } "Paldea-Blaze" -> {
                    speciesWithForm = "Paldean $species (Blaze)"
                    thumbnailLinkRawName = "${species}_paldea_blaze_breed"
                } "Paldea-Aqua" -> {
                    speciesWithForm = "Paldean $species (Aqua)"
                    thumbnailLinkRawName = "${species}_paldea_aqua_breed"
                } else -> {
                    speciesWithForm = "$species issue"
                    thumbnailLinkRawName = "${species}"
                }
                }
            } else {
                speciesWithForm = "$formVariation $species"
                thumbnailLinkRawName = "$formVariation $species"
            }
            when (formVariation) {
                "Paldean" -> {
                    thumbnailLinkRawName = "${species}_paldea"
                }
                "Alolan" -> {
                    thumbnailLinkRawName = "${species}_alola"
                } "Galarian" -> {
                thumbnailLinkRawName = "${species}_galar"
                } "Hisuian" -> {
                    thumbnailLinkRawName = "${species}_hisui"
                }
            }
        }
        val serverLabels = Config.data.server.labelCheck
        val pokemonLabel = pokemon.form.labels.firstOrNull { it in serverLabels}

        val shiny = pokemon.shiny
        val thumbnailLinkName = normalise(thumbnailLinkRawName)
        val thumbnailURL = if (shiny) {
            Config.data.sprites[thumbnailLinkName]?.shiny
        } else {
            Config.data.sprites[thumbnailLinkName]?.standard
        }

        val ivs = pokemon.ivs
        val hp = ivs[Stats.HP] ?: 0
        val atk = ivs[Stats.ATTACK] ?: 0
        val def = ivs[Stats.DEFENCE] ?: 0
        val spatk = ivs[Stats.SPECIAL_ATTACK] ?: 0
        val spdef = ivs[Stats.SPECIAL_DEFENCE] ?: 0
        val speed = ivs[Stats.SPEED] ?: 0

        val ivList = listOf(hp, atk, def, spatk, spdef, speed)
        val perfectCount = ivList.count { it == 31 }
        val perfectIV = if (perfectCount == 6) true else false

        var labels = listOf("")
        if (perfectCount == 6 && shiny) {
            labels = listOf("perfect", "shiny")
        } else if (perfectCount == 6) {
            labels = listOf("perfect")
        } else if (shiny) {
            labels = listOf("shiny")
        }

        val catchEmAllPlayers = Config.getCatchEmAllPlayers()
        val affectedCatchEmAllPlayers = checkCatchEmAll(playerNames = catchEmAllPlayers, pokemon = pokemon)

        return PokemonStream(
            species = species,
            speciesWithForm = speciesWithForm,
            speciesForm = formVariation?: "",
            thumbnail = thumbnailURL?: "https://media1.tenor.com/m/ZQvpE8_p-hMAAAAC/pokemon-confused.gif",
            shiny = shiny,
            perfectIV = perfectIV,
            pokemonLabel = pokemonLabel,
            hatchingLabels = labels,
            catchEmAllPlayers = affectedCatchEmAllPlayers
        )
    }

    fun checkCatchEmAll(playerNames: List<String>, pokemon: Pokemon): List<String> {
        val allowed = playerNames.map { it.lowercase() }.toSet()
        var potentialPlayers: List<String> = listOf()
        ProjectAsh.server!!.playerList.players.forEach { player ->
            if (player.scoreboardName.lowercase() in allowed) {
                val isNew = isNewCatch(pokemon, player)
                if (isNew) { potentialPlayers = potentialPlayers + player.scoreboardName }
            }
        }
        return potentialPlayers
    }

    fun isNewCatch(pokemon: Pokemon, serverPlayer: ServerPlayer): Boolean {
        val dex = Cobblemon.playerDataManager.getPokedexData(serverPlayer)

        val speciesRecord = dex.getSpeciesRecord(pokemon.species.resourceIdentifier)
            ?: return true

        val dexEntry = Dexes.dexEntryMap[cobblemonResource("national")]
            ?.getEntries()
            ?.firstOrNull { it.speciesId == pokemon.species.resourceIdentifier }
            ?: return true

        dexEntry.forms.forEach { f ->
            FileLogger.log("  displayForm='${f.displayForm}' unlockForms=${f.unlockForms}")
        }

        val matchingForm = dexEntry.forms
            .firstOrNull { it.displayForm.equals(pokemon.form.name, ignoreCase = true) }
            ?: return true

        return matchingForm.unlockForms.none { unlockFormKey ->
            val isShinyKey = unlockFormKey.contains("shiny", ignoreCase = true)
            if (pokemon.shiny != isShinyKey) return@none false
            val formRecord = speciesRecord.getFormRecord(unlockFormKey) ?: return@none false
            formRecord.knowledge == PokedexEntryProgress.CAUGHT
        }
    }

    private fun normalise(name: String): String =
        name.trim().lowercase()
            .replace(' ', '-')    // "Mr Mime" -> "mr-mime"
            .replace(":", "-")    // "Type: Null" -> "type-null"
            .replace(".", "")     // "Mr. Mime" -> "mr-mime"
            .replace("'", "")     // "Farfetch'd" -> "farfetchd"
            .replace("é", "e")    // "Flabébé" -> "flabebe"
            .replace("♀", "-f")   // "Nidoran♀" -> "nidoran-f"
            .replace("♂", "-m")   // "Nidoran♂" -> "nidoran-m"
}