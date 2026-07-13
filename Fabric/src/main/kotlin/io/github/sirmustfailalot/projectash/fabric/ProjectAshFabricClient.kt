package io.github.sirmustfailalot.projectash.fabric

import net.fabricmc.api.ClientModInitializer

object ProjectAshFabricClient : ClientModInitializer {

    override fun onInitializeClient() {
        ModKeyBindings.register()
    }
}