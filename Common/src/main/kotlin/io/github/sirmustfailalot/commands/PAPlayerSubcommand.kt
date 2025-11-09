package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

/** Returns the node that hangs under: /projectash player <here> */
interface PAPlayerSubcommand {
    fun build(): LiteralArgumentBuilder<CommandSourceStack>
}