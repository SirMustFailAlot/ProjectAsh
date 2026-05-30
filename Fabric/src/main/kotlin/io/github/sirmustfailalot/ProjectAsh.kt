package io.github.sirmustfailalot


import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object ProjectAsh : ModInitializer {
    private val logger = LoggerFactory.getLogger("project-ash")
    var server: MinecraftServer? = null
    override fun onInitialize() {

    }
}