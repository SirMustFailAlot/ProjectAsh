package io.github.sirmustfailalot.projectash.showcase

import net.minecraft.world.item.ItemStack

object ChatPreviewManager {
    var hoveredPreviewStack: ItemStack? = null
    var hoveredTexturePath: String? = null
    fun clear() {
        hoveredPreviewStack = null
        hoveredTexturePath = null
    }
}