package io.github.sirmustfailalot.projectash.subscribers

// Cobblemon Classes
import com.cobblemon.mod.common.api.events.CobblemonEvents

// Minecraft Classes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

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

        CobblemonEvents.POKEMON_CAPTURED.subscribe { ev ->
            val serverPlayer: ServerPlayer = ev.player
            val pokemon = ev.pokemon
            SpawnLifecycle.onCapture(
                serverPlayer = serverPlayer,
                pokemon = pokemon
            )
        }

        CobblemonEvents.POKEMON_FAINTED.subscribe { ev ->
            val pokemon = ev.pokemon
            SpawnLifecycle.onFainted(
                pokemon = pokemon
            )
        }

    }
}