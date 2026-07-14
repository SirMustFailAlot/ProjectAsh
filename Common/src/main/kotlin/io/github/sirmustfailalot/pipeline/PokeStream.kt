package io.github.sirmustfailalot.projectash.pipeline

// Project Ash Classes
import io.github.sirmustfailalot.projectash.config.Config

// Cobblemon Classes
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.toVec3d
import com.cobblemon.mod.common.item.PokemonItem

// Minecraft Classes
import net.minecraft.server.level.ServerLevel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

// Java Classes
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID

object PokeStream {
    private val logger = LoggerFactory.getLogger("ProjectAsh")

    data class PokemonLifespan(
        // Spawning and Entity Context
        val isWild: Boolean,
        val pokemon: Pokemon,
        val uuidPokemon: UUID? = null,
        val uuidEntity: WeakReference<PokemonEntity>? = null,
        val spawnSource: String,
        val spawnDimension: String,
        val spawnPos: String,
        val spawnClosestPlayer: String,

        // Pokémon Species
        val species: String,
        val speciesWithForm: String,
        val speciesForm: String,

        // Pokémon Aspects
        val resourceIdentifier: ResourceLocation? = null,
        val hasLabels: String,
        val isShiny: Boolean,
        val isPerfectIV: Boolean,
        val sprite: String = "",
        val pokemonItemStack: ItemStack,

        var evaluationResult: RuleEvaluationResult? = null
    )

    fun pokeGlance(
        pokemonEntity: PokemonEntity,
        pokemonSpawnedBy: String
    ): PokemonLifespan {
        var isWild = pokemonEntity.pokemon.isWild()
        val uuidPokemon = pokemonEntity.pokemon.uuid
        var uuidEntity: WeakReference<PokemonEntity>? = null
        var world: ServerLevel? = null
        var spawnDimension: String? = null
        var spawnPos: String? = null
        var spawnClosestPlayer: String? = null
        if (uuidPokemon != null) {
            uuidEntity = WeakReference(pokemonEntity)
            world = (pokemonEntity.commandSenderWorld as? ServerLevel)
            if (world != null) {
                spawnDimension = when {
                    world.dimension().toString().contains("overworld") -> "Overworld"
                    world.dimension().toString().contains("the_nether") -> "Nether"
                    world.dimension().toString().contains("the_end") -> "End"
                    else -> "Unknown"
                }
                val pos = pokemonEntity.blockPosition()
                spawnPos = pos.x.toString() + ", " + pos.y.toString() + ", " + pos.z.toString()
                val players = world.players()
                    .filter { it.isAlive }
                    .minByOrNull {
                        it.position().distanceToSqr(pos.toVec3d())
                    }
                spawnClosestPlayer = players?.name?.string ?: ""
            }
        }

        // Run common mappings
        return buildLifespanSnapshot(
            isWild = isWild,
            pokemon = pokemonEntity.pokemon,
            uuidPokemon = uuidPokemon,
            uuidEntity = uuidEntity,
            spawnSource = pokemonSpawnedBy,
            spawnDimension = spawnDimension ?: "",
            spawnPos = spawnPos ?: "",
            spawnClosestPlayer = spawnClosestPlayer ?: ""
        )
    }

    // NEW HATCHGLANCE (Entity-Free Method designed specifically for Eggs)
    fun hatchGlance(
        pokemon: Pokemon,
        pokemonSpawnedBy: String
    ): PokemonLifespan {
        // Eggs do not have world coordinate contexts, default to safe inventory titles
        return buildLifespanSnapshot(
            isWild = false,
            pokemon = pokemon,
            uuidPokemon = pokemon.uuid, // Pull safe non-null data UUID directly
            uuidEntity = null,          // No world entity exists
            spawnSource = pokemonSpawnedBy,
            spawnDimension = "N/A",
            spawnPos = "Inventory",
            spawnClosestPlayer = "N/A"
        )
    }

    // Shared internal utility helper to run your naming, mapping, and sprite calculations uniformly
    private fun buildLifespanSnapshot(
        isWild: Boolean,
        pokemon: Pokemon,
        uuidPokemon: UUID?,
        uuidEntity: WeakReference<PokemonEntity>?,
        spawnSource: String,
        spawnDimension: String,
        spawnPos: String,
        spawnClosestPlayer: String
    ): PokemonLifespan {
        val species = pokemon.species.translatedName.string
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
                "Paldean" -> { thumbnailLinkRawName = "${species}_paldea" }
                "Alolan" -> { thumbnailLinkRawName = "${species}_alola" }
                "Galarian" -> { thumbnailLinkRawName = "${species}_galar" }
                "Hisuian" -> { thumbnailLinkRawName = "${species}_hisui" }
            }
        }

        val pokemonResourceIdentifier = pokemon.species.resourceIdentifier
        val pokemonItemStack: ItemStack = PokemonItem.from(pokemon)
        val pokemonLabelCheck = listOf("legendary", "mythical", "ultra_beast", "paradox")
        val pokemonLabelRaw = pokemon.form.labels.firstOrNull { it in pokemonLabelCheck } ?: ""
        val pokemonLabels = when (pokemonLabelRaw.lowercase()) {
            "legendary" -> "Legendary"
            "mythical" -> "Mythical"
            "paradox" -> "Paradox"
            "ultra_beast" -> "Ultra Beast"
            else -> ""
        }
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

        val spriteName = normalise(thumbnailLinkRawName)
        val sprite = if (isShiny) {
            Config.data.sprites[spriteName]?.shiny
        } else {
            Config.data.sprites[spriteName]?.standard
        }

        return PokemonLifespan(
            isWild = isWild,
            pokemon = pokemon,
            uuidPokemon = uuidPokemon,
            uuidEntity = uuidEntity,
            spawnSource = spawnSource,
            spawnDimension = spawnDimension,
            spawnPos = spawnPos,
            spawnClosestPlayer = spawnClosestPlayer,
            species = species,
            speciesWithForm = speciesWithForm,
            speciesForm = formVariation ?: "",
            resourceIdentifier = pokemonResourceIdentifier,
            pokemonItemStack = pokemonItemStack,
            hasLabels = pokemonLabels,
            isShiny = isShiny,
            isPerfectIV = isPerfectIV,
            sprite = sprite ?: "https://media1.tenor.com/m/ZQvpE8_p-hMAAAAC/pokemon-confused.gif"
        )
    }

    private fun normalise(name: String): String =
        name.trim().lowercase()
            .replace(' ', '-')
            .replace(":", "-")
            .replace(".", "")
            .replace("'", "")
            .replace("é", "e")
            .replace("♀", "-f")
            .replace("♂", "-m")
}