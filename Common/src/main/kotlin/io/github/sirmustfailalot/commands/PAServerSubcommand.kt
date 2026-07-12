package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

interface PAServerSubcommand {
    fun build(): LiteralArgumentBuilder<CommandSourceStack>
}
