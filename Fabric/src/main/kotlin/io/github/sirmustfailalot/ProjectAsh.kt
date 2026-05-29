package io.github.sirmustfailalot


import net.fabricmc.api.ModInitializer
import io.github.sirmustfailalot.battle.TrainerBattleTracker
import io.github.sirmustfailalot.utility.FabricLoggerImpl

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
import io.github.sirmustfailalot.hatching.HatchAnnounce
import io.github.sirmustfailalot.utility.FileLogger
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.server.level.ServerPlayer

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

        FileLogger.platformImpl = FabricLoggerImpl()
        FileLogger.log("Project Ash Initialized")

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            ProjectAshCommand.register(dispatcher)
        }

        // Spawning and Loading Pokemon
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe{ ev ->
            val spawnPokemonEntity = ev.entity
            SpawnTracker.onSpawn(pokemonEntity = spawnPokemonEntity, spawnSource = "Known")
        }
        ServerEntityEvents.ENTITY_LOAD.register { loadPokemonEntity, _ ->
            if (loadPokemonEntity is PokemonEntity && Config.data.server.checkUnknownSpawns) {
                val spawnCause = loadPokemonEntity.spawnCause == null || loadPokemonEntity.spawnCause?.javaClass?.simpleName?.contains("Command", ignoreCase = true)?: false
                if (spawnCause) {
                    SpawnTracker.onSpawn(pokemonEntity = loadPokemonEntity, spawnSource = "Unknown")
                }
            }
        }

//        CobblemonEvents.POKEMON_CAPTURED.subscribe { ev ->
//            val player: ServerPlayer = ev.player
//            val pokemon = ev.pokemon
//            SpawnTracker.onCapture(player=player, pokemon=pokemon)
//        }
//
//        CobblemonEvents.POKEMON_FAINTED.subscribe(Priority.LOWEST, SpawnTracker::onFainted)
//
//
//        ServerEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
//            if (entity is PokemonEntity) {
//                SpawnTracker.onRemoved(entity, entity.removalReason)
//            }
//        }
//
//        CobblemonEvents.HATCH_EGG_POST.subscribe(Priority.LOWEST, HatchAnnounce::onHatch)

        CobblemonEvents.BATTLE_STARTED_POST.subscribe { event ->
            TrainerBattleTracker.onBattleStarted(event)
        }

        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            TrainerBattleTracker.onBattleCompleted(event)
        }
    }
}