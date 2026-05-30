package io.github.sirmustfailalot.projectash.subscribers

// Cobblemon
import com.cobblemon.mod.common.api.events.CobblemonEvents

// Minecraft
import net.minecraft.server.MinecraftServer

object EventSubscribers {
    fun startSubscribers() {
        spawnCycleListeners()
    }

    var server: MinecraftServer? = null
    fun spawnCycleListeners() {
        // Spawning Entities into the world
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe { event ->
            SpawnLifecycle.onSpawn(event.entity)
        }
    }
}