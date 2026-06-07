package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.ChatFormatting

object PlayerCatchEmAll : PAPlayerSubcommand {
    private val BOOL_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, b -> b.suggest("false"); b.suggest("true"); b.buildFuture() }

    /** Helper to get the executing player's name or fail gracefully if not a player. */
    private fun executingPlayerNameOrFail(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>): String? {
        return try {
            ctx.source.playerOrException.scoreboardName
        } catch (_: Exception) {
            ctx.source.sendSuccess(
                { Component.literal("[Project Ash] This command must be run by a player in-game.") }, false
            )
            null
        }
    }

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("CatchEmAll")
            // ProjectAsh Player CatchEmAll LocalSpawnsOnly? true/false - Turns on/off CatchEmAll for the player, regardless of other settings.
            .then(literal("Enabled?")
                .then(literal("enabled")
                    .executes { ctx ->
                        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                        Config.toggleCatchEmAllModeEnabled(player, true)
                            ctx.source.sendSuccess({Component.literal("[Project Ash] CatchEmAll Mode: TRUE").withStyle(ChatFormatting.GREEN)}, false)
                        1
                    }
                )
                .then(literal("disabled")
                    .executes { ctx ->
                        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                        Config.toggleCatchEmAllModeEnabled(player, false)
                        ctx.source.sendSuccess({Component.literal("[Project Ash] CatchEmAll Mode: FALSE").withStyle(ChatFormatting.RED)}, false)
                        1
                    }
                )
            )

            // ProjectAsh Player CatchEmAll LocalSpawnsOnly? true/false - Turns on/off CatchEmAll for the player's local spawns only
            .then(literal("LocalSpawnsOnly?")
                .then(literal("enabled")
                    .executes { ctx ->
                        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                        Config.toggleCatchEmAllModeLocal(player, true)
                        ctx.source.sendSuccess({Component.literal("[Project Ash] CatchEmAll Local Spawns Only?: TRUE").withStyle(ChatFormatting.GREEN)}, false)
                        1
                    }
                )
                .then(literal("disabled")
                    .executes { ctx ->
                        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                        Config.toggleCatchEmAllModeLocal(player, false)
                        ctx.source.sendSuccess({Component.literal("[Project Ash] CatchEmAll Local Spawns Only?: FALSE").withStyle(ChatFormatting.RED)}, false)
                        1
                    }
                )
            )
}