package io.github.sirmustfailalot.projectash.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object ProjectAshFabricClient : ClientModInitializer {
    val showcaseKeyBind: KeyMapping = KeyMapping(
        "key.projectash.showcase",
        GLFW.GLFW_KEY_H,
        "category.projectash"
    )

    override fun onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(showcaseKeyBind)
    }
}