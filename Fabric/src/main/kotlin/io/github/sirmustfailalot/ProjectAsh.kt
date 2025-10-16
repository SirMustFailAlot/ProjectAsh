package io.github.sirmustfailalot

import net.fabricmc.api.ModInitializer

// Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

// World
import net.minecraft.server.MinecraftServer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory

// Commands
import io.github.sirmustfailalot.projectash.commands.ProjectAshCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

interface PASubcommand {
    /** Return the literal node to hang under /projectash */
    fun build(): LiteralArgumentBuilder<CommandSourceStack>
}

object ProjectAsh : ModInitializer {
    private val logger = LoggerFactory.getLogger("project-ash")
    var server: MinecraftServer? = null
    override fun onInitialize() {
        logger.info("Project Ash ----------- *Clears Throat*, is this thing on? *Taps Mic* Bogies")
        Config.init()

        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            server = srv
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            server = null
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            ProjectAshCommand.register(dispatcher)
        }

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.LOWEST, SpawnTracker::onSpawn)
        CobblemonEvents.POKEMON_CAPTURED.subscribe { ev ->
            val player: ServerPlayer = ev.player
            val pokemon = ev.pokemon
            SpawnTracker.onCapture(player=player, pokemon=pokemon)
        }

        CobblemonEvents.POKEMON_FAINTED.subscribe(Priority.LOWEST, SpawnTracker::onFainted)

        // 4) Vanilla removal (to detect natural despawns)
        //ServerEntityEvents.ENTITY_UNLOAD.register { entity, _world ->
        //    if (entity is PokemonEntity) {
        //        SpawnTracker.onRemoved(entity, entity.removalReason)
        //    }
        //}
    }
}