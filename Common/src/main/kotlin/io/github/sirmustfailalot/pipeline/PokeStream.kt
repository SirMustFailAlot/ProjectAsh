package io.github.sirmustfailalot.projectash.pipeline

// Project Ash Classes
import io.github.sirmustfailalot.projectash.config.Config

// Cobblemon Classes
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.toVec3d

// Minecraft Classes
import net.minecraft.server.level.ServerLevel

// Java Classes
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID

object PokeStream {
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
        val hasLabels: String,
        val isShiny: Boolean,
        val isPerfectIV: Boolean,
        val sprite: String = "",

        var evaluationResult: RuleEvaluationResult? = null
    )

    fun pokeGlance(
        pokemonEntity: PokemonEntity,
        pokemonSpawnedBy: String
    ): PokemonLifespan {
        // Spawning and Entity Context
        val uuidPokemon = pokemonEntity.pokemon.uuid
        val uuidEntity = WeakReference(pokemonEntity)
        val spawnSource = pokemonSpawnedBy
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

        // Pokmemon Species
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

        // sprite
        val spriteName = normalise(thumbnailLinkRawName)
        val sprite = if (isShiny) {
            Config.data.sprites[spriteName]?.shiny
        } else {
            Config.data.sprites[spriteName]?.standard
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
            hasLabels = pokemonLabels,
            isShiny = isShiny,
            isPerfectIV = isPerfectIV,
            sprite = sprite?: "https://media1.tenor.com/m/ZQvpE8_p-hMAAAAC/pokemon-confused.gif",
        )
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