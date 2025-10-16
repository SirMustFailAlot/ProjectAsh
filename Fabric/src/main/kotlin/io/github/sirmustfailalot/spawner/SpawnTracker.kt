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
import io.github.sirmustfailalot.Discord
import io.github.sirmustfailalot.ProjectAsh
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory
import java.util.Locale

object SpawnTracker {
    private val logger = LoggerFactory.getLogger("project-ash")
    private val tracked = java.util.concurrent.ConcurrentHashMap<UUID, Tracked>() // key: entityUuid

    data class Tracked(
        val pokemonUuid: UUID,
        val spawntype: List<String>,
        val species: String,
        val speciesForm: String,
        val closestplayer: String,
        val spawnedAt: Long = System.currentTimeMillis(),
        @Volatile var outcome: Outcome? = null,
        val ref: WeakReference<PokemonEntity>
    )

    enum class Outcome { CAUGHT, FAINTED, NATURAL_DESPAWN }

    // Grab the server for announcing
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

    // ---- lifecycle hooks you wire up to events ----

    fun onSpawn(spawn: SpawnEvent<PokemonEntity>) {
        val pokeUuid = spawn.entity.pokemon.uuid
        val world = ctxServerLevel(spawn.ctx) ?: return
        val dimensionName = world.dimension().toString()
        val dimension = when {
            world.dimension().toString().contains("overworld") -> "Overworld"
            world.dimension().toString().contains("the_nether") -> "Nether"
            world.dimension().toString().contains("the_end") -> "End"
            else -> return
        }
        val pos = spawn.ctx.position
        val posValue = pos.x.toString() + ", " + pos.y.toString() + ", " + pos.z.toString()
        val players = spawn.ctx.world.players()
            .filter { it.isAlive }
            .minByOrNull { it.position().distanceToSqr(pos.toVec3d()) } // Use player's position for distance calculation
        val playerName = players?.name?.string?:""
        val pokemon = spawn.entity.pokemon
        val shiny = pokemon.shiny
        val labelsWeWant = listOf("legendary")
        val label = pokemon.form.labels.firstOrNull { it in labelsWeWant}
        val spawnType = when {
            shiny && label != null -> listOf("Shiny", "Legendary")
            shiny && label == null -> listOf("Shiny")
            !shiny && label != null -> listOf("Legendary")
            else -> listOf("DO NOT TRACK")
        }
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

        tracked[pokeUuid] = Tracked(
            pokemonUuid = pokeUuid,
            spawntype = spawnType,
            closestplayer = playerName,
            species = spawn.entity.pokemon.species.name,
            speciesForm = speciesPlusForm,
            ref = WeakReference(spawn.entity)
        )

        if (spawnType.firstOrNull() == "DO NOT TRACK") {
            tracked.remove(pokeUuid)
        } else {
            logger.info("Spawn: $pokeUuid - $spawnType - $species")
            Announcement.spawn(ProjectAsh.server, dimension, playerName, spawnType, speciesPlusForm, posValue)
            Discord.spawn(ProjectAsh.server, dimension, playerName, spawnType, shiny, species, speciesPlusForm, posValue)
        }
    }

    fun onCapture(player: ServerPlayer, pokemon: Pokemon) {
        val t = findTracked(pokemon.uuid) ?: return
        Announcement.capture(ProjectAsh.server, t.closestplayer, t.spawntype, t.species)
        Discord.captureOrFainted(eventType="Captured", server=ProjectAsh.server, playerName=player.gameProfile.name, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm)
        tracked.remove(pokemon.uuid)
    }

    fun onFainted(capture: PokemonFaintedEvent) {
        val t = findTracked(capture.pokemon.uuid) ?: return
        Announcement.fainted(ProjectAsh.server, t.spawntype, t.species)
        Discord.captureOrFainted(eventType="Fainted", server=ProjectAsh.server, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm)
        tracked.remove(capture.pokemon.uuid)
    }


    /** Call from ENTITY_UNLOAD or equivalent */
    //fun onRemoved(entity: PokemonEntity, removalReason: Entity.RemovalReason?) {
    //    val t = tracked[entity.uuid] ?: return
    //    if (t.outcome == null) {
    //        t.outcome = Outcome.NATURAL_DESPAWN
    //    }
    //    finalizeAndCleanup(t, reason = "removed:${removalReason?.name}")
    //}

    // ---- helpers ----
    private fun findTracked(pokemonUuid: UUID): Tracked? {
        return tracked[pokemonUuid]?.takeIf { it.outcome == null }
    }
}
