package io.github.sirmustfailalot

import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

object Announcement {
    private val logger = LoggerFactory.getLogger("ProjectAsh")

    fun send(server: MinecraftServer?, dimension: String, playerName: String?, spawnType: String, species: String, posValue: String) {
        val message = "$playerName! $spawnType $species $dimension at $posValue"
        val IngameEnabled = Config.data.in_game.enabled

        if (IngameEnabled) {
            server.let { server ->
                server?.playerList?.players?.forEach { p ->
                    p.sendSystemMessage(Component.literal(message))
                }
            }
        }
    }

    fun discordWebhookFail(server: MinecraftServer?) {
        logger.info("Project Ash: discord_webhook missing in config; skipping webhook send.")
        val message = "Project Ash: discord_webhook missing in config; skipping webhook send."

        server.let { server ->
            server?.playerList?.players?.forEach { p ->
                p.sendSystemMessage(Component.literal(message))
            }
        }
    }

}