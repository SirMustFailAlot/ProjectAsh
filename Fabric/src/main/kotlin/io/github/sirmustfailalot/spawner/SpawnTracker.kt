import com.cobblemon.mod.common.api.events.battles.BattleEvent
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonFaintedEvent
import java.lang.ref.WeakReference
import java.util.UUID
import net.minecraft.world.entity.Entity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.toVec3d
import io.github.sirmustfailalot.Announcement
import io.github.sirmustfailalot.Config
import io.github.sirmustfailalot.Discord
import io.github.sirmustfailalot.ProjectAsh
import io.github.sirmustfailalot.utility.PokemonUtility

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.Locale
import kotlin.String
import kotlin.collections.contains
import kotlin.text.lowercase
import kotlin.toString

object SpawnTracker {
    private val logger = LoggerFactory.getLogger("project-ash")
    private val tracked = java.util.concurrent.ConcurrentHashMap<UUID, Tracked>() // key: entityUuid
    private val scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1)

    data class Tracked(
        val announceSource: String,
        val announceTarget: String,
        val announcePlayers: List<String> = listOf(""),
        val catchEmAllPlayers: List<String> = listOf(""),
        val pokemonUuid: UUID,
        val spawntype: List<String>,
        val species: String,
        val speciesForm: String,
        val closestplayer: String,
        val spawnedAt: Long = System.currentTimeMillis(),
        val thumbnailURL: String,
        @Volatile var outcome: Outcome? = null,
        val ref: WeakReference<PokemonEntity>
    )

    data class spawnResult(
        val announceSource: String,
        val announceTarget: String = "Do not announce",
        val announcePlayers: List<String> = listOf(""),
        val catchEmAllPlayers: List<String> = listOf(""),
        val pokemonUuid: UUID,
        val dimension: String = "",
        val pos: String = "",
        val closestPlayer: String = "",
        val shiny: Boolean = false,
        val spawnType: List<String> = listOf("Do not announce"),
        val species: String = "",
        val speciesPlusForm: String = "",
        val thumbnailURL: String
    )

    enum class Outcome { CAUGHT, FAINTED, NATURAL_DESPAWN }

    fun spawnCheckRule(context: PokemonEntity, spawnSource: String): spawnResult? {
        // Pokemon Stuff!
        val entity = context
        val pokemon = entity.pokemon
        val pokeUuid = pokemon.uuid
        val world = entity.commandSenderWorld as? ServerLevel ?: return null
        val dimension = when {
            world.dimension().toString().contains("overworld") -> "Overworld"
            world.dimension().toString().contains("the_nether") -> "Nether"
            world.dimension().toString().contains("the_end") -> "End"
            else -> return null
        }
        val pos = entity.blockPosition()
        val posValue = pos.x.toString() + ", " + pos.y.toString() + ", " + pos.z.toString()
        val players = world.players()
            .filter { it.isAlive }
            .minByOrNull { it.position().distanceToSqr(pos.toVec3d()) } // Use player's position for distance calculation
        val playerName = players?.name?.string?:""

        val pokeGlance = PokemonUtility.quickGlance(entity)

        // Server Check
        val serverConfig = Config.data.server
        var shinyRule = serverConfig.shinyCheck && pokeGlance.shiny
        var unknownRule = serverConfig.checkUnknownSpawns
        var allowUnknownSpawns = if (!unknownRule && spawnSource == "Unknown") { false } else { true }
        val spawnType = when {
            shinyRule && pokeGlance.pokemonLabel != null -> listOf("Shiny", pokeGlance.pokemonLabel)
            shinyRule && pokeGlance.pokemonLabel == null -> listOf("Shiny")
            !shinyRule && pokeGlance.pokemonLabel != null -> listOf(pokeGlance.pokemonLabel)
            else -> listOf("DO NOT TRACK")
        }

        // SERVER - Labels and Shiny Check
        if (allowUnknownSpawns && (shinyRule || spawnType.firstOrNull() != "DO NOT TRACK")) {
            return spawnResult(
                announceSource = spawnSource,
                announceTarget= "Server",
                announcePlayers = listOf(""),
                catchEmAllPlayers = pokeGlance.catchEmAllPlayers,
                pokemonUuid = pokeUuid,
                dimension = dimension,
                pos = posValue,
                closestPlayer = playerName,
                shiny = pokeGlance.shiny,
                spawnType = spawnType,
                species = pokeGlance.species,
                speciesPlusForm = pokeGlance.speciesWithForm,
                thumbnailURL = pokeGlance.thumbnail
            )
        }

        // SERVER - Special Checks
        val hasServerSpecialMatch = Config.data.server.specialCheck.any {
            it.speciesName.equals(pokeGlance.species, ignoreCase = true) &&
                    (!it.shinyCheck || pokeGlance.shiny)
        }

        if (allowUnknownSpawns && hasServerSpecialMatch) {
            return spawnResult(
                announceSource = spawnSource,
                announceTarget = "Server",
                announcePlayers = listOf(""), // or omit if not used for server
                catchEmAllPlayers = pokeGlance.catchEmAllPlayers,
                pokemonUuid = pokeUuid,
                dimension = dimension,
                pos = posValue,
                closestPlayer = playerName,
                shiny = pokeGlance.shiny,
                spawnType = listOf("special"),
                species = pokeGlance.species,
                speciesPlusForm = pokeGlance.speciesWithForm,
                thumbnailURL = pokeGlance.thumbnail
            )
        }

        // PLAYERS - Special Checks
        val matchedPlayers = findPlayersForSpecial(pokeGlance.species, pokeGlance.shiny)
        if (matchedPlayers.isNotEmpty()) {
            return spawnResult(
                announceSource = spawnSource,
                announceTarget = "Players",
                announcePlayers = matchedPlayers,
                catchEmAllPlayers = pokeGlance.catchEmAllPlayers,
                pokemonUuid = pokeUuid,
                dimension = dimension,
                pos = posValue,
                closestPlayer = playerName,
                shiny = pokeGlance.shiny,
                spawnType = listOf("special"),
                species = pokeGlance.species,
                speciesPlusForm = pokeGlance.speciesWithForm,
                thumbnailURL = pokeGlance.thumbnail
            )
        }

        return spawnResult(
            announceSource = spawnSource,
            announceTarget = "Do not announce",
            announcePlayers = matchedPlayers,
            catchEmAllPlayers = pokeGlance.catchEmAllPlayers,
            pokemonUuid = pokeUuid,
            dimension = dimension,
            pos = posValue,
            closestPlayer = playerName,
            shiny = pokeGlance.shiny,
            spawnType = listOf("catchEmCheck"),
            species = pokeGlance.species,
            speciesPlusForm = pokeGlance.speciesWithForm,
            thumbnailURL = pokeGlance.thumbnail
        )
    }

    fun onSpawnUnknown(spawn: PokemonEntity) {
        val spawnCheck = spawnCheckRule(context = spawn, spawnSource = "Unknown") ?: return
        tracked[spawnCheck.pokemonUuid] = Tracked(
            announceSource = spawnCheck.announceSource,
            announceTarget = spawnCheck.announceTarget,
            announcePlayers = spawnCheck.announcePlayers,
            pokemonUuid = spawnCheck.pokemonUuid,
            spawntype = spawnCheck.spawnType,
            closestplayer = spawnCheck.closestPlayer,
            species = spawnCheck.species,
            speciesForm = spawnCheck.speciesPlusForm,
            thumbnailURL = spawnCheck.thumbnailURL,
            catchEmAllPlayers = spawnCheck.catchEmAllPlayers,
            ref = WeakReference(spawn)
        )

        if (spawnCheck.announceTarget == "Server") {
            Announcement.spawn(announceSource = spawnCheck.announceSource, announceTarget = spawnCheck.announceTarget, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, catchEmAllPlayers = spawnCheck.catchEmAllPlayers, posValue = spawnCheck.pos)
            Discord.spawn(ProjectAsh.server, announceSource = spawnCheck.announceSource, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, shiny = spawnCheck.shiny , species = spawnCheck.species, speciesPlusForm = spawnCheck.speciesPlusForm, posValue = spawnCheck.pos, thumbnailURL = spawnCheck.thumbnailURL)
        } else if (spawnCheck.announceTarget == "Players") {
            Announcement.spawn(announceSource = spawnCheck.announceSource, announceTarget = spawnCheck.announceTarget, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, catchEmAllPlayers = spawnCheck.catchEmAllPlayers, posValue = spawnCheck.pos)
        } else if (spawnCheck.announceTarget == "Do not announce" && !spawnCheck.catchEmAllPlayers.isEmpty()) {
            Announcement.spawn(announceSource = spawnCheck.announceSource, announceTarget = spawnCheck.announceTarget, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, catchEmAllPlayers = spawnCheck.catchEmAllPlayers, posValue = spawnCheck.pos)
        }
    }

    fun onSpawn(spawn: SpawnEvent<PokemonEntity>) {
        val spawnCheck = spawnCheckRule(context = spawn.entity, spawnSource = "Known") ?: return
        tracked[spawnCheck.pokemonUuid] = Tracked(
            announceSource = spawnCheck.announceSource,
            announceTarget = spawnCheck.announceTarget,
            announcePlayers = spawnCheck.announcePlayers,
            pokemonUuid = spawnCheck.pokemonUuid,
            spawntype = spawnCheck.spawnType,
            closestplayer = spawnCheck.closestPlayer,
            species = spawnCheck.species,
            speciesForm = spawnCheck.speciesPlusForm,
            ref = WeakReference(spawn.entity),
            catchEmAllPlayers = spawnCheck.catchEmAllPlayers,
            thumbnailURL = spawnCheck.thumbnailURL
        )

        if (spawnCheck.announceTarget == "Server") {
        Announcement.spawn(announceSource = spawnCheck.announceSource, announceTarget = spawnCheck.announceTarget, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, catchEmAllPlayers = spawnCheck.catchEmAllPlayers, posValue = spawnCheck.pos)
        Discord.spawn(ProjectAsh.server, announceSource = spawnCheck.announceSource, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, shiny = spawnCheck.shiny , species = spawnCheck.species, speciesPlusForm = spawnCheck.speciesPlusForm, posValue = spawnCheck.pos, thumbnailURL = spawnCheck.thumbnailURL)
        } else if (spawnCheck.announceTarget == "Players") {
            Announcement.spawn(announceSource = spawnCheck.announceSource, announceTarget = spawnCheck.announceTarget, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, catchEmAllPlayers = spawnCheck.catchEmAllPlayers, posValue = spawnCheck.pos)
        } else if (spawnCheck.announceTarget == "Do not announce" && !spawnCheck.catchEmAllPlayers.isEmpty()) {
            Announcement.spawn(announceSource = spawnCheck.announceSource, announceTarget = spawnCheck.announceTarget, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, catchEmAllPlayers = spawnCheck.catchEmAllPlayers, posValue = spawnCheck.pos)
        }
    }

    // ---- captures etc ----
    fun onCapture(player: ServerPlayer, pokemon: Pokemon) {
        val t = findTracked(pokemon.uuid) ?: return
        tracked.remove(pokemon.uuid)
        if (t.announceTarget == "Server") {
            Announcement.capture(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, player.gameProfile.name, t.spawntype, t.species)
            Discord.announcement(eventType="Captured", server=ProjectAsh.server, playerName=player.gameProfile.name, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm, thumbnailURL=t.thumbnailURL)
        } else if (t.announceTarget == "Players") {
            Announcement.capture(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, player.gameProfile.name, t.spawntype, t.species)
        }
    }
    fun onFainted(capture: PokemonFaintedEvent) {
        val t = findTracked(capture.pokemon.uuid) ?: return
        tracked.remove(capture.pokemon.uuid)
        if (t.announceTarget == "Server") {
            Announcement.fainted(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
            Discord.announcement(eventType="Fainted", server=ProjectAsh.server, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm, thumbnailURL=t.thumbnailURL)
        } else if (t.announceTarget == "Players") {
            Announcement.fainted(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
        }
    }
    fun onRemoved(entity: PokemonEntity, removalReason: Entity.RemovalReason?) {
        val pokeUuid = entity.pokemon.uuid

        scheduler.schedule({
            ProjectAsh.server?.execute {
                val t = tracked[pokeUuid] ?: return@execute

                if (t.outcome == null) {
                    tracked.remove(pokeUuid)
                    if (t.announceTarget == "Server") {
                        Announcement.removed(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
                        Discord.announcement(
                            eventType = "Despawned",
                            server = ProjectAsh.server,
                            spawnType = t.spawntype,
                            species = t.species,
                            speciesPlusForm = t.speciesForm,
                            thumbnailURL=t.thumbnailURL
                        )
                    } else if (t.announceTarget == "Players") {
                        Announcement.removed(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
                    }
                }
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS)
    }

    // ---- helpers ----
    private fun findTracked(pokemonUuid: UUID): Tracked? {
        return tracked[pokemonUuid]?.takeIf { it.outcome == null }
    }

    /** Return the list of player names whose Special rules match this spawn. */
    fun findPlayersForSpecial(species: String, shiny: Boolean): List<String> {
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
}
