package io.github.sirmustfailalot.spawner

import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.api.spawning.context.SpawningContext
import com.cobblemon.mod.common.util.toVec3d
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import io.github.sirmustfailalot.Announcement
import io.github.sirmustfailalot.Discord
import io.github.sirmustfailalot.ProjectAsh
import org.slf4j.LoggerFactory
import java.util.Locale

object Spawner {
    private val logger = LoggerFactory.getLogger("project-ash")
    /** Works on Cobblemon where SpawningContext has getLevel() (newer) or getWorld() (older). */
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

    fun handle(spawn: SpawnEvent<PokemonEntity>) {
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
        val playerName = players?.name?.string
        val pokemon = spawn.entity.pokemon
        val shiny = pokemon.shiny
        val labelsWeWant = listOf("legendary")
        val label = pokemon.form.labels.firstOrNull { it in labelsWeWant}
        val spawnType = when {
            shiny && label != null -> "Shiny Legendary"
            shiny && label == null -> "Shiny"
            !shiny && label != null -> "Legendary"
            else -> return // Fallback case
        }

        val formVariation: String? = pokemon.form.labels
            .asSequence()
            .map { it.toString() }
            .firstOrNull { it.contains("_form", ignoreCase = true) }
            ?.substringBefore("_form")
            ?.replaceFirstChar { it.titlecase(Locale.ROOT) }

        val species = pokemon.species.translatedName.string

        val speciesPlusForm = if (formVariation.isNullOrBlank()) {
            species
        } else {
            "$species ($formVariation)"
        }

        Announcement.send(ProjectAsh.server, dimension, playerName, spawnType, speciesPlusForm, posValue)
        Discord.send(dimension, playerName, spawnType, shiny, species, speciesPlusForm, posValue)
    }
}