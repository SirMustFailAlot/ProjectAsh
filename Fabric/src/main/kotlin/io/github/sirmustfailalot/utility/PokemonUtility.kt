package io.github.sirmustfailalot.utility

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokedex.Dexes
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.toVec3d
import io.github.sirmustfailalot.Config
import io.github.sirmustfailalot.ProjectAsh
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.String
import java.util.UUID
import kotlin.text.contains
import kotlin.text.lowercase


object PokemonUtility {
    data class PokemonLifespan(
        // Spawning and Entity Context
        val uuidPokemon: UUID,
        val uuidEntity: WeakReference<PokemonEntity>,
        val spawnSource: String,
        val spawnDimension: String,
        val spawnPos: String,
        val spawnClosestPlayer: String,

        // Pokémon Species
        val species: String,
        val speciesWithForm: String,
        val speciesForm: String,

        // Pokémon Aspects
        val hasLabels: List<*>,
        val isShiny: Boolean,
        val isPerfectIV: Boolean,

        //  Announcement Checks
        val isServerAnnouncement: Boolean,
        val isServerSpecial: Boolean,
        val hasServerLabel: String,
        val hasSpecialPlayers: Boolean,
        val targetSpecialPlayers: List<String>,
        val hasCatchEmAllPlayers: Boolean,
        val targetCatchEmAllPlayers: List<String>,

        // Discord Thumbnail
        val thumbnail: String = "",
    )

    fun quickGlance(
        pokemonEntity: PokemonEntity,
        spawnSource: String,
    ): PokemonLifespan {
        // Spawning and Entity Context
        val uuidPokemon = pokemonEntity.pokemon.uuid
        val uuidEntity = WeakReference(pokemonEntity)
        val spawnSource = spawnSource
        val world = (pokemonEntity.commandSenderWorld as? ServerLevel)!!
        val spawnDimension = when {
            world.dimension().toString().contains("overworld") -> "Overworld"
            world.dimension().toString().contains("the_nether") -> "Nether"
            world.dimension().toString().contains("the_end") -> "End"
            else -> "Unknown"
        }
        val pos = pokemonEntity.blockPosition()
        val spawnPos = pos.x.toString() + ", " + pos.y.toString() + ", " + pos.z.toString()
        val players = world.players()
            .filter { it.isAlive }
            .minByOrNull { it.position().distanceToSqr(pos.toVec3d()) } // Use player's position for distance calculation
        val spawnClosestPlayer = players?.name?.string?:""

        // Pokemon Species
        val pokemon = pokemonEntity.pokemon
        val species = pokemonEntity.pokemon.species.translatedName.string
        val formVariation: String? = pokemon.form.labels
            .asSequence()
            .map { it }
            .firstOrNull { it.contains("_form", ignoreCase = true) }
            ?.substringBefore("_form")
            ?.replaceFirstChar { it.titlecase(Locale.ROOT) }
        var nonOriginalFormFound = false
        if (!formVariation.isNullOrBlank()) {
            pokemon.features.forEach {
                if (it.name.trim().equals(formVariation.trim(), ignoreCase = true)) {
                    nonOriginalFormFound = true
                }
            }
        }
        var speciesWithForm: String
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
                    thumbnailLinkRawName = "$species"
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

        // Pokémon Aspects
        val pokemonLabelCheck = listOf("legendary", "mythical", "ultra-beast", "paradox")
        val pokemonLabels = pokemon.form.labels.firstOrNull { it in pokemonLabelCheck } ?: ""

        val serverWantsShiny = Config.data.server.shinyCheck
        val isShiny = pokemon.shiny
        val ivs = pokemon.ivs
        val hp = ivs[Stats.HP] ?: 0
        val atk = ivs[Stats.ATTACK] ?: 0
        val def = ivs[Stats.DEFENCE] ?: 0
        val spatk = ivs[Stats.SPECIAL_ATTACK] ?: 0
        val spdef = ivs[Stats.SPECIAL_DEFENCE] ?: 0
        val speed = ivs[Stats.SPEED] ?: 0
        val ivList = listOf(hp, atk, def, spatk, spdef, speed)
        val perfectCount = ivList.count { it == 31 }
        val isPerfectIV = perfectCount == 6

        // Announcement Details - Server Checks
        val serverLabels = Config.data.server.labelCheck
        val hasServerLabel = pokemon.form.labels.firstOrNull { it in serverLabels} ?: ""
        val isServerSpecial = Config.data.server.specialCheck.any {
            it.speciesName.equals(species, ignoreCase = true) &&
                    (!it.shinyCheck || isShiny)
        }
        val isServerAnnouncement: Boolean = hasServerLabel.isNotEmpty() || isServerSpecial || (serverWantsShiny && isShiny)

        // Announcement Details - Special Players
        val targetSpecialPlayers = checkSpecialPlayers(species, isShiny)
        val hasSpecialPlayers: Boolean = targetSpecialPlayers.isNotEmpty()

        // Announcement Details - Catch Em All Players
        val targetCatchEmAllPlayers = checkCatchEmAllPlayers(pokemon = pokemon)
        val hasCatchEmAllPlayers: Boolean = targetCatchEmAllPlayers.isNotEmpty()

        // Announcement Details - Discord
        val thumbnailLinkName = normalise(thumbnailLinkRawName)
        val thumbnailURL = if (isShiny) {
            Config.data.sprites[thumbnailLinkName]?.shiny
        } else {
            Config.data.sprites[thumbnailLinkName]?.standard
        }

        return PokemonLifespan(
            // Spawning and Entity Context
            uuidPokemon = uuidPokemon,
            uuidEntity = uuidEntity,
            spawnSource = spawnSource,
            spawnDimension = spawnDimension,
            spawnPos = spawnPos,
            spawnClosestPlayer = spawnClosestPlayer,

            // Pokémon Species
            species = species,
            speciesWithForm = speciesWithForm,
            speciesForm = formVariation ?: "",

            // Pokémon Aspects
            hasLabels = pokemonLabels as List<*>,
            isShiny = isShiny,
            isPerfectIV = isPerfectIV,

            //  Announcement Details
            isServerAnnouncement = isServerAnnouncement,
            isServerSpecial = isServerSpecial,
            hasServerLabel = hasServerLabel,
            hasSpecialPlayers = hasSpecialPlayers,
            targetSpecialPlayers = targetSpecialPlayers,
            hasCatchEmAllPlayers = hasCatchEmAllPlayers,
            targetCatchEmAllPlayers = targetCatchEmAllPlayers,

            // Discord Thumbnail
            thumbnail = thumbnailURL?: "https://media1.tenor.com/m/ZQvpE8_p-hMAAAAC/pokemon-confused.gif",
        )
    }

    /** Return the list of player names whose Special rules match this spawn. */
    fun checkSpecialPlayers(species: String, shiny: Boolean): List<String> {
        val s = species.trim().lowercase()
        return Config.data.player.entries
            .asSequence()
            // Optional: only consider players who have enabled their rules
            .filter { (_, p) -> p.enabled }
            // Match if any rule hits: same species, and shinyOnly implies shiny
            .filter { (_, p) ->
                p.specialCheck.any { rule ->
                    rule.speciesName.equals(s, ignoreCase = true) &&
                            (!rule.shinyCheck || shiny)
                }
            }
            .map { (name, _) -> name }
            .toList()
    }

    fun checkCatchEmAllPlayers(pokemon: Pokemon): List<String> {
        val playerNames = Config.getCatchEmAllPlayers()
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