package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerDiscordThumbnails : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server DiscordThumbnails <boolean>
        literal("DiscordThumbnails")
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setServerDiscordThumbnails(true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord thumbnails: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setServerDiscordThumbnails(false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord thumbnails: DISABLED").withStyle(ChatFormatting.RED) }, true)
                    1
                }
            )
}