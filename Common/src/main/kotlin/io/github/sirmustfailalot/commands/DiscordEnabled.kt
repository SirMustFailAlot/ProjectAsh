package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object DiscordEnabled : PASubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("DiscordEnabled")
            .requires { it.hasPermission(3) } // OPs only
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setDiscordEnabled(true)
                    ctx.source.sendSuccess({ Component.literal("Discord announcements: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setDiscordEnabled(false)
                    ctx.source.sendSuccess({ Component.literal("Discord announcements: DISABLED") }, true)
                    1
                }
            )
}