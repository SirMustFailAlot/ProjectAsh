package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

interface PAPlayerSubcommand {
    fun build(): LiteralArgumentBuilder<CommandSourceStack>
}
