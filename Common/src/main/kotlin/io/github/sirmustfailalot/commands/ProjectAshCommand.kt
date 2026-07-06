package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ProjectAshCommand {

    // Register your server and player subcommands here:
    private val serverSubs: List<PAServerSubcommand> = listOf(
        ServerDiscordEnabled,
        ServerDiscordUpdateWebhook,
        ServerDiscordThumbnails,
        ServerInGameEnabled,
        ServerPerfectChecks,
        ServerShinyChecks,
        ServerCheckUnknownSpawns,
        ServerLabelChecks,
        ServerSpecialChecks,
        ServerBlacklistChecks
    )

    private val playerSubs: List<PAPlayerSubcommand> = listOf(
        PlayerSpecialChecks,
        PlayerCatchEmAll
    )

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = literal("ProjectAsh")

        // /projectash server ...
        val serverRoot = literal("Server")
            .requires { it.hasPermission(3) } // lock server settings to ops
        serverSubs.forEach { sub -> serverRoot.then(sub.build()) }
        root.then(serverRoot)

        // /projectash player ...
        val playerRoot = literal("Player")
        playerSubs.forEach { sub -> playerRoot.then(sub.build()) }
        root.then(playerRoot)

        dispatcher.register(root)
    }
}
