package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.projectash.config.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerPerfectChecks : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server PerfectIV <boolean>
        literal("PerfectIV")
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setServerPerfectCheck(true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Perfect IV Checks: ENABLED") }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setServerPerfectCheck(false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Perfect IV: DISABLED") }, true)
                    1
                }
            )
}