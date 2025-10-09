package io.github.sirmustfailalot
import io.github.sirmustfailalot.spawner.Spawner

import net.fabricmc.api.ModInitializer

// Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents

// World
import net.minecraft.server.MinecraftServer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory

// Commands
import io.github.sirmustfailalot.projectash.commands.ProjectAshCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

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

        // Get the Server Information
        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            server = srv
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            server = null
        }

        // Create a spawn handle
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.LOWEST, Spawner::handle)

        // Register Commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            ProjectAshCommand.register(dispatcher)
        }
    }
}
