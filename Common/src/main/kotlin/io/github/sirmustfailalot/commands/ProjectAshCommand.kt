package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ProjectAshCommand {
    // Add subcommands here
    private val subs: List<PASubcommand> = listOf(
        DiscordUpdateWebook,
        DiscordEnabled,
        DiscordThumbnails,
        InGameEnabled
        // Add more…
    )

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = literal("projectash")
        subs.forEach { sub -> root.then(sub.build()) }
        dispatcher.register(root)
    }
}