package io.github.sirmustfailalot.projectash.fabric

import io.github.sirmustfailalot.platform.services.PlatformHelper
import io.github.sirmustfailalot.projectash.showcase.ShowcasePayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.item.ItemStack

class FabricPlatformHelper : PlatformHelper {
    override val platformName: String = "Fabric"
    override fun isModLoaded(modId: String): Boolean = FabricLoader.getInstance().isModLoaded(modId)
    override val isDevelopmentEnvironment: Boolean = FabricLoader.getInstance().isDevelopmentEnvironment

    override fun sendShowcasePacketToServer(itemStack: ItemStack) {
        if (!itemStack.isEmpty) {
            ClientPlayNetworking.send(ShowcasePayload(itemStack))
        }
    }
}