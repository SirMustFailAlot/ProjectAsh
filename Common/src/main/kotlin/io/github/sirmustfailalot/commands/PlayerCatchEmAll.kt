package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

import io.github.sirmustfailalot.Config.toggleCatchEmAllMode
import io.github.sirmustfailalot.projectash.commands.PAPlayerSubcommand

object PlayerCatchEmAll : PAPlayerSubcommand {
    /** Helper to get the executing player's name or fail gracefully if not a player. */
    private fun executingPlayerNameOrFail(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>): String? {
        return try {
            ctx.source.playerOrException.scoreboardName
        } catch (_: Exception) {
            ctx.source.sendSuccess(
                { Component.literal("[Project Ash] This command must be run by a player in-game.") }, false)
            null
        }
    }

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("CatchEmAll")
            .then(literal("enabled")
                .executes { ctx ->
                    val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                    toggleCatchEmAllMode(player, true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] CatchEmAll: ENABLED").withStyle(ChatFormatting.GREEN) }, false)
                     1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                    toggleCatchEmAllMode(player, false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] CatchEmAll: DISABLED").withStyle(ChatFormatting.RED) }, false)
                    1
                }
            )
}