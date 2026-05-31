package io.github.sirmustfailalot

// Project Ash Classes
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers
import io.github.sirmustfailalot.projectash.commands.ProjectAshCommand

// Cobblemon Classes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import io.github.sirmustfailalot.projectash.subscribers.SpawnLifecycle

// Fabric Classes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents

object ProjectAshFabric : ModInitializer {
    override fun onInitialize() {
        ProjectAsh.initialise()

        // Load the Minecraft Server Variable
        ServerLifecycleEvents.SERVER_STARTED.register { srv -> EventSubscribers.server = srv}
        ServerLifecycleEvents.SERVER_STOPPED.register { EventSubscribers.server = null }

        // Register Commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            ProjectAshCommand.register(dispatcher)
        }

        // Load Common Subscribers and Fabric Specific Subscribers
        EventSubscribers.startSubscribers()
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is PokemonEntity) {
                SpawnLifecycle.onSpawn(
                    pokemonEntity = entity,
                    spawnReason = "Unknown"
                )
            }
        }
    }
}