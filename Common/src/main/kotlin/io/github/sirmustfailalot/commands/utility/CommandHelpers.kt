package io.github.sirmustfailalot.projectash.commands.utility

import com.mojang.brigadier.context.CommandContext
import io.github.sirmustfailalot.projectash.config.ShinyFlag
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component

fun executingPlayerNameOrFail(ctx: CommandContext<CommandSourceStack>): String? =
    try {
        ctx.source.playerOrException.scoreboardName
    } catch (_: Exception) {
        ctx.source.sendFailure(
            Component.literal("[Project Ash] This command must be run by a player in-game.")
        )
        null
    }

fun parseShinyFlag(value: String): ShinyFlag? =
    when (value.trim().lowercase()) {
        "include" -> ShinyFlag.INCLUDE
        "exclude" -> ShinyFlag.EXCLUDE
        "only" -> ShinyFlag.ONLY
        else -> null
    }
