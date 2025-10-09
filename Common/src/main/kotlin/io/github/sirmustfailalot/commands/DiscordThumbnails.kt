package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object DiscordThumbnails : PASubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("DiscordThumbnails")
            .requires { it.hasPermission(3) } // OPs only
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setDiscordThumbnails(true)
                    ctx.source.sendSuccess({ Component.literal("Discord thumbnails: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setDiscordThumbnails(false)
                    ctx.source.sendSuccess({ Component.literal("Discord thumbnails: DISABLED") }, true)
                    1
                }
            )
}