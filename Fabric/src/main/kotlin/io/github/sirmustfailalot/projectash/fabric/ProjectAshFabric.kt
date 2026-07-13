package io.github.sirmustfailalot.projectash.fabric

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import io.github.sirmustfailalot.ProjectAsh
import io.github.sirmustfailalot.projectash.commands.ProjectAshCommand
import io.github.sirmustfailalot.projectash.showcase.ShowcasePayload
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers
import io.github.sirmustfailalot.projectash.subscribers.SpawnLifecycle
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import io.github.sirmustfailalot.projectash.showcase.ShowcaseServerProcessor

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object ProjectAshFabric : ModInitializer {
    override fun onInitialize() {
        PayloadTypeRegistry.playC2S().register(ShowcasePayload.TYPE, ShowcasePayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(ShowcasePayload.TYPE) { payload, context ->
            val player = context.player()
            val server = context.server()

            val incomingStack = payload.itemStack

            server.execute {
                if (!incomingStack.isEmpty) {
                    ShowcaseServerProcessor.processShowcaseRequest(player, incomingStack)
                }
            }
        }

        // Load the Minecraft Server Variable
        ServerLifecycleEvents.SERVER_STARTED.register { srv -> EventSubscribers.server = srv}
        ServerLifecycleEvents.SERVER_STOPPED.register { EventSubscribers.server = null }

        // Register Commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            ProjectAshCommand.register(dispatcher)
        }

        // Load Common Subscribers and Fabric Specific Subscribers
        ProjectAsh.initialise()
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is PokemonEntity) {
                SpawnLifecycle.onSpawn(
                    pokemonEntity = entity,
                    spawnReason = "Unknown"
                )
            }
        }
        ServerEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            if (entity is PokemonEntity) {
                val pokemonUUID = entity.pokemon.uuid
                val removalReasonString = entity.removalReason?.toString() ?: "Unknown"

                SpawnLifecycle.scheduler.schedule({
                    try {
                        SpawnLifecycle.onRemoved(
                            pokemonEntityUUID = pokemonUUID,
                            removalReason = removalReasonString
                        )
                    } catch (e: Exception) {
                        LoggerFactory.getLogger("ProjectAsh")
                            .error("Error executing delayed onRemoved for ${entity.pokemon.species.translatedName.string}", e)
                    }
                }, 2, TimeUnit.SECONDS) // <-- 2 Seconds Delay
            }
        }
    }
}