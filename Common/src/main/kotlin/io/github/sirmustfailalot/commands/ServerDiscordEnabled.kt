package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerDiscordEnabled : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server DiscordEnabled <boolean>
        literal("DiscordEnabled")
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setServerDiscordEnabled(true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord announcements: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setServerDiscordEnabled(false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Discord announcements: DISABLED") }, true)
                    1
                }
            )
}