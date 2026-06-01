package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.projectash.config.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerShinyChecks : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server Shiny <boolean>
        literal("Shiny")
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setServerShinyCheck(true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Shiny Checks: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setServerShinyCheck(false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Shiny Checks: DISABLED") }, true)
                    1
                }
            )
}