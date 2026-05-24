package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerInGameEnabled : PAServerSubcommand {
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server InGameEnabled <boolean>
        literal("InGameEnabled")
            .then(literal("enabled")
                .executes { ctx ->
                    Config.setServerIngameEnabled(true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] In-game announcements: ENABLED").withStyle(ChatFormatting.GREEN) }, true)
                    1
                }
            )
            .then(literal("disabled")
                .executes { ctx ->
                    Config.setServerIngameEnabled(false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] In-game announcements: DISABLED").withStyle(ChatFormatting.RED) }, true)
                    1
                }
            )
}