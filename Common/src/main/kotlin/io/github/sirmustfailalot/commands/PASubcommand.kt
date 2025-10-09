package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

interface PASubcommand {
    /** Return the literal node to hang under /projectash */
    fun build(): LiteralArgumentBuilder<CommandSourceStack>
}