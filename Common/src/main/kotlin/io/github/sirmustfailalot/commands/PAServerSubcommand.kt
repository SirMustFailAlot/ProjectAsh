package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

/** Returns the node that hangs under: /projectash server <here> */
interface PAServerSubcommand {
    fun build(): LiteralArgumentBuilder<CommandSourceStack>
}