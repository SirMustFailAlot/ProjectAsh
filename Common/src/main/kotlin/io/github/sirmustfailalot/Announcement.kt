package io.github.sirmustfailalot

import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

object Announcement {
    fun send(server: MinecraftServer?, dimension: String, playerName: String?, spawnType: String, species: String, posValue: String) {
        val message = "$playerName! $spawnType $species $dimension at $posValue"

        server.let { server ->
            server?.playerList?.players?.forEach { p ->
                p.sendSystemMessage(Component.literal(message))
            }
        }
    }

}