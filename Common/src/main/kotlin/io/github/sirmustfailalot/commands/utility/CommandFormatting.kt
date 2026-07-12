package io.github.sirmustfailalot.projectash.commands.utility

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

fun statusText(enabled: Boolean): String = if (enabled) "ENABLED" else "DISABLED"

fun statusComponent(enabled: Boolean): Component =
    Component.literal(statusText(enabled)).withStyle(
        if (enabled) ChatFormatting.GREEN else ChatFormatting.RED
    )

fun prefixedStatus(label: String, enabled: Boolean): Component =
    Component.literal("[Project Ash] $label: ").append(statusComponent(enabled))

fun neutralMessage(message: String): Component =
    Component.literal("[Project Ash] $message").withStyle(ChatFormatting.YELLOW)
