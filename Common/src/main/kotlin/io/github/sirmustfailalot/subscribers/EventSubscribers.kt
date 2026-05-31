package io.github.sirmustfailalot.projectash.subscribers

// Cobblemon Classes
import com.cobblemon.mod.common.api.events.CobblemonEvents

// Minecraft Classes
import net.minecraft.server.MinecraftServer

object EventSubscribers {
    fun startSubscribers() {
        spawnCycleListeners()
    }

    var server: MinecraftServer? = null
    fun spawnCycleListeners() {
        // Spawning Entities into the world
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe { event ->
            SpawnLifecycle.onSpawn(
                pokemonEntity = event.entity,
                spawnReason = "Known"
            )
        }
    }
}