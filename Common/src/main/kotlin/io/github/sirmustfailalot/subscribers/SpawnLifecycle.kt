package io.github.sirmustfailalot.projectash.subscribers

// Cobblemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

// Logger
import org.slf4j.LoggerFactory

object SpawnLifecycle {
    private val logger = LoggerFactory.getLogger("project-ash")
    fun onSpawn(entity: PokemonEntity) {
        logger.info("Spawning Shit is happenning {}", entity.pokemon.species.translatedName)
    }
}