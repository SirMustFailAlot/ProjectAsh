package io.github.sirmustfailalot.projectash.client

import io.github.sirmustfailalot.platform.Services
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

object CommonClientHandler {
    var currentlyHoveredCard: ItemStack? = null

    fun attemptShowcase(itemStack: ItemStack) {
        if (itemStack.isEmpty) return

        val registryName = BuiltInRegistries.ITEM.getKey(itemStack.item)
        if (registryName.namespace == "cobbletcg") {
            Services.PLATFORM.sendShowcasePacketToServer(itemStack)
        }
    }
}