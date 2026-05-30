package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.projectash.config.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerCheckUnknownSpawns : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server Shiny <boolean>
        literal("SpawnCommands")
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setCheckUnknownSpawns(true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Checking Unknown Spawns: ENABLED").withStyle(ChatFormatting.GREEN) }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setCheckUnknownSpawns(false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Checking Unknown Spawns: DISABLED").withStyle(ChatFormatting.RED) }, true)
                    1
                }
            )
}