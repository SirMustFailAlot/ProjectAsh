package io.github.sirmustfailalot.projectash.fabric

import com.mojang.blaze3d.platform.InputConstants
import io.github.sirmustfailalot.projectash.client.CommonClientHandler
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object ModKeyBindings {
    val showcaseKeyBind: KeyMapping = KeyMapping(
        "key.projectash.showcase",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        "key.categories.projectash"
    )

    fun register() {
        KeyBindingHelper.registerKeyBinding(showcaseKeyBind)

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player != null && client.screen == null) {
                while (showcaseKeyBind.consumeClick()) {
                    val mainHandItem = client.player!!.mainHandItem
                    if (!mainHandItem.isEmpty) {
                        CommonClientHandler.attemptShowcase(mainHandItem)
                    }
                }
            }
        }
    }
}