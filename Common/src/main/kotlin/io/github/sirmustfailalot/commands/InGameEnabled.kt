package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object InGameEnabled : PASubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("InGameEnabled")
            .requires { it.hasPermission(3) } // OPs only
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setIngameEnabled(true)
                    ctx.source.sendSuccess({ Component.literal("In-game announcements: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setIngameEnabled(false)
                    ctx.source.sendSuccess({ Component.literal("In-game announcements: DISABLED") }, true)
                    1
                }
            )
}