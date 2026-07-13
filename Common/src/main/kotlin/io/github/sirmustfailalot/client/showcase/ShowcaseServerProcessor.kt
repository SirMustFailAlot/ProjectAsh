package io.github.sirmustfailalot.projectash.showcase

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.ChatFormatting

object ShowcaseServerProcessor {

    fun processShowcaseRequest(player: ServerPlayer, itemStack: ItemStack) {
        if (itemStack.isEmpty) return

        val itemNameComponent = itemStack.hoverName

        val itemHoverData = HoverEvent.ItemStackInfo(itemStack)
        val customHoverEvent = HoverEvent(HoverEvent.Action.SHOW_ITEM, itemHoverData)

        val broadcastMessage = Component.empty()
            .append(Component.literal("[★] ").withStyle { style -> style.withColor(0xFFAA00).withBold(true) })
            .append(player.displayName!!.copy().withStyle { style -> style.withColor(0xFFAA00).withBold(false) })
            .append(Component.literal(" is showing off ").withStyle { style -> style.withColor(ChatFormatting.WHITE).withBold(false) })
            .append(
                Component.literal("[").append(itemNameComponent).append("]")
                    .withStyle { style -> style.withColor(0x3ce6bd).withHoverEvent(customHoverEvent).withBold(false) }
            )

        player.server.playerList.broadcastSystemMessage(broadcastMessage, false)
    }
}