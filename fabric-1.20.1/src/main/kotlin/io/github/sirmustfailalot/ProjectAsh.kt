package io.github.sirmustfailalot

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

// Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents

// Spawn Handler
import io.github.sirmustfailalot.spawnHandler.SpawnHandler
import com.google.gson.JsonParser
import java.io.File

object ProjectAsh : ModInitializer {
    private val logger = LoggerFactory.getLogger("project-ash")
	val discordWebhookURL = ""
	override fun onInitialize() {
		logger.info("Project Ash ----------- Oh Dang, Did we Load, Heck Yeah!")
		CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.LOWEST, SpawnHandler::handle)

	}
}