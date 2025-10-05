package io.github.sirmustfailalot.spawner

import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.toVec3d
import io.github.sirmustfailalot.ProjectAsh
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object Spawner {
    private val logger = LoggerFactory.getLogger("project-ash")

    fun handle(spawn: SpawnEvent<PokemonEntity>) {

        // Get Basic Information e.g. world, position, Pokémon
        val world = spawn.ctx.world
        val dimensionName = world.dimension().toString()
        val dimension = when {
            world.dimension().toString().contains("overworld") -> "Overworld"
            world.dimension().toString().contains("the_nether") -> "Nether"
            world.dimension().toString().contains("the_end") -> "End"
            else -> return
        }

        // Get Position
        val pos = spawn.ctx.position
        val posValue = pos.x.toString() + ", " + pos.y.toString() + ", " + pos.z.toString()

        // Get Nearest Player - FIXED VERSION
        val players = spawn.ctx.world.players()
            .filter { it.isAlive }
            .minByOrNull { it.position().distanceToSqr(pos.toVec3d()) } // Use player's position for distance calculation

        // Check to see if it's a shiny or a label that we like
        val pokemon = spawn.entity.pokemon
        val shiny = pokemon.shiny
        val labelsWeWant = listOf("legendary")
        val label = pokemon.form.labels.firstOrNull { it in labelsWeWant}

        val spawntype = when {
            shiny && label != null -> "Shiny Legendary"
            shiny && label == null -> "Shiny"
            !shiny && label != null -> "Legendary"
            else -> return // Fallback case
        }

        val species = pokemon.species.translatedName.string

        val message = players?.name?.string + "! " + spawntype + " " + species + " " + dimension + " at " + posValue
        logger.info(message)

        ProjectAsh.server?.let { server ->
            server.playerList.players.forEach { p ->
                p.sendSystemMessage(Component.literal(message))
            }
        }
    }
}