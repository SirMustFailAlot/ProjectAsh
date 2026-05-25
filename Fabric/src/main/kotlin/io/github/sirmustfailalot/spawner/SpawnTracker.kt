import com.cobblemon.mod.common.api.events.battles.BattleEvent
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonFaintedEvent
import com.cobblemon.mod.common.api.spawning.context.SpawningContext
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
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.Locale
import kotlin.String
import kotlin.collections.contains
import kotlin.toString

object SpawnTracker {
    private val logger = LoggerFactory.getLogger("project-ash")
    private val tracked = java.util.concurrent.ConcurrentHashMap<UUID, Tracked>() // key: entityUuid
    private val scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1)

    data class Tracked(
        val announceType: String,
        val announcePlayers: List<String> = listOf(""),
        val pokemonUuid: UUID,
        val spawntype: List<String>,
        val species: String,
        val speciesForm: String,
        val closestplayer: String,
        val spawnedAt: Long = System.currentTimeMillis(),
        @Volatile var outcome: Outcome? = null,
        val ref: WeakReference<PokemonEntity>
    )

    data class spawnResult(
        val announceType: String = "Do not announce",
        val announcePlayers: List<String> = listOf(""),
        val pokemonUuid: UUID,
        val dimension: String = "",
        val pos: String = "",
        val closestPlayer: String = "",
        val shiny: Boolean = false,
        val spawnType: List<String> = listOf("Do not announce"),
        val species: String = "",
        val speciesPlusForm: String = ""
    )

    enum class Outcome { CAUGHT, FAINTED, NATURAL_DESPAWN }

    private fun ctxServerLevel(ctx: SpawningContext): ServerLevel? {
        // Try getLevel()
        try {
            val m = ctx.javaClass.getMethod("getLevel")
            when (val v = m.invoke(ctx)) {
                is ServerLevel -> return v
                is Level -> return v as? ServerLevel
            }
        } catch (_: NoSuchMethodException) { /* fall through */ }
        // Fallback getWorld()
        return try {
            val m = ctx.javaClass.getMethod("getWorld")
            m.invoke(ctx) as? ServerLevel
        } catch (t: Throwable) {
            logger.info("Project Ash: could not resolve ServerLevel from SpawningContext (${t.javaClass.simpleName}: ${t.message})")
            null
        }
    }

    fun spawnCheckRule(context: SpawnEvent<PokemonEntity>): spawnResult? {
        // Pokemon Stuff!
        val pokeUuid = context.entity.pokemon.uuid
        val world = ctxServerLevel(context.ctx) ?: return null
        val dimension = when {
            world.dimension().toString().contains("overworld") -> "Overworld"
            world.dimension().toString().contains("the_nether") -> "Nether"
            world.dimension().toString().contains("the_end") -> "End"
            else -> return null
        }
        val pos = context.ctx.position
        val posValue = pos.x.toString() + ", " + pos.y.toString() + ", " + pos.z.toString()
        val players = context.ctx.world.players()
            .filter { it.isAlive }
            .minByOrNull { it.position().distanceToSqr(pos.toVec3d()) } // Use player's position for distance calculation
        val playerName = players?.name?.string?:""
        val pokemon = context.entity.pokemon
        val shiny = pokemon.shiny
        val species = pokemon.species.translatedName.string
        val formVariation: String? = pokemon.form.labels
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

        // Server Check
        val serverConfig = Config.data.server
        var shinyRule = serverConfig.shinyCheck && shiny
        val serverLabels = Config.data.server.labelCheck
        val label = pokemon.form.labels.firstOrNull { it in serverLabels}
        val spawnType = when {
            shinyRule && label != null -> listOf("Shiny", label)
            shinyRule && label == null -> listOf("Shiny")
            !shinyRule && label != null -> listOf(label)
            else -> listOf("DO NOT TRACK")
        }

        // SERVER - Labels and Shiny Check
        if (shinyRule || spawnType.firstOrNull() != "DO NOT TRACK") {
            return spawnResult(
                announceType= "Server",
                announcePlayers = listOf(""),
                pokemonUuid = pokeUuid,
                dimension = dimension,
                pos = posValue,
                closestPlayer = playerName,
                shiny = shiny,
                spawnType = spawnType,
                species = species,
                speciesPlusForm = speciesPlusForm
            )
        }

        // SERVER - Special Checks
        val hasServerSpecialMatch = Config.data.server.specialCheck.any {
            it.speciesName.equals(species, ignoreCase = true) &&
                    (!it.shinyCheck || shiny)
        }

        if (hasServerSpecialMatch) {
            return spawnResult(
                announceType = "Server",
                announcePlayers = listOf(""), // or omit if not used for server
                pokemonUuid = pokeUuid,
                dimension = dimension,
                pos = posValue,
                closestPlayer = playerName,
                shiny = shiny,
                spawnType = listOf("special"),
                species = species,
                speciesPlusForm = speciesPlusForm
            )
        }

        // PLAYERS - Special Checks
        val matchedPlayers = findPlayersForSpecial(species, shiny)
        if (matchedPlayers.isNotEmpty()) {
            return spawnResult(
                announceType = "Players",
                announcePlayers = matchedPlayers,
                pokemonUuid = pokeUuid,
                dimension = dimension,
                pos = posValue,
                closestPlayer = playerName,
                shiny = shiny,
                spawnType = listOf("special"),
                species = species,
                speciesPlusForm = speciesPlusForm
            )
        }

        return spawnResult(
            announceType= "Do not announce",
            announcePlayers = listOf(""),
            pokemonUuid = pokeUuid,
            dimension = "",
            pos = "",
            closestPlayer = "",
            shiny = true,
            spawnType = listOf(""),
            species = "",
            speciesPlusForm = ""
        )
    }

    fun onSpawn(spawn: SpawnEvent<PokemonEntity>) {

        val spawnCheck = spawnCheckRule(spawn)
        if (spawnCheck?.announceType == "Do not announce" || spawnCheck == null) {return}

        tracked[spawnCheck.pokemonUuid] = Tracked(
            announceType = spawnCheck.announceType,
            announcePlayers = spawnCheck.announcePlayers,
            pokemonUuid = spawnCheck.pokemonUuid,
            spawntype = spawnCheck.spawnType,
            closestplayer = spawnCheck.closestPlayer,
            species = spawnCheck.species,
            speciesForm = spawnCheck.speciesPlusForm,
            ref = WeakReference(spawn.entity)
        )

        if (spawnCheck.announceType == "Server") {
        Announcement.spawn(announceType = spawnCheck.announceType, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, posValue = spawnCheck.pos)
        Discord.spawn(ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, shiny = spawnCheck.shiny , species = spawnCheck.species, speciesPlusForm = spawnCheck.speciesPlusForm, posValue = spawnCheck.pos)
        } else if (spawnCheck.announceType == "Players") {
            Announcement.spawn(announceType = spawnCheck.announceType, announcePlayers = spawnCheck.announcePlayers, server = ProjectAsh.server, dimension = spawnCheck.dimension, playerName = spawnCheck.closestPlayer, spawnType = spawnCheck.spawnType, species = spawnCheck.speciesPlusForm, posValue = spawnCheck.pos)
        }
    }

    // ---- captures etc ----
    fun onCapture(player: ServerPlayer, pokemon: Pokemon) {
        val t = findTracked(pokemon.uuid) ?: return
        tracked.remove(pokemon.uuid)
        if (t.announceType == "Server") {
            Announcement.capture(announceType = t.announceType, announcePlayers = t.announcePlayers, ProjectAsh.server, player.gameProfile.name, t.spawntype, t.species)
            Discord.announcement(eventType="Captured", server=ProjectAsh.server, playerName=player.gameProfile.name, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm)
        } else if (t.announceType == "Players") {
            Announcement.capture(announceType = t.announceType, announcePlayers = t.announcePlayers, ProjectAsh.server, player.gameProfile.name, t.spawntype, t.species)
        }
    }
    fun onFainted(capture: PokemonFaintedEvent) {
        val t = findTracked(capture.pokemon.uuid) ?: return
        tracked.remove(capture.pokemon.uuid)
        if (t.announceType == "Server") {
            Announcement.fainted(announceType = t.announceType, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
            Discord.announcement(eventType="Fainted", server=ProjectAsh.server, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm)
        } else if (t.announceType == "Players") {
            Announcement.fainted(announceType = t.announceType, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
        }
    }
    fun onRemoved(entity: PokemonEntity, removalReason: Entity.RemovalReason?) {
        val pokeUuid = entity.pokemon.uuid

        scheduler.schedule({
            ProjectAsh.server?.execute {
                val t = tracked[pokeUuid] ?: return@execute

                if (t.outcome == null) {
                    tracked.remove(pokeUuid)
                    if (t.announceType == "Server") {
                        Announcement.removed(announceType = t.announceType, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
                        Discord.announcement(
                            eventType = "Despawned",
                            server = ProjectAsh.server,
                            spawnType = t.spawntype,
                            species = t.species,
                            speciesPlusForm = t.speciesForm
                        )
                    } else if (t.announceType == "Players") {
                        Announcement.removed(announceType = t.announceType, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
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
