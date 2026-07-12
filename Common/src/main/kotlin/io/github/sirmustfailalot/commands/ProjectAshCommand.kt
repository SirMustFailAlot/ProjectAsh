package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.CommandDispatcher
import io.github.sirmustfailalot.projectash.commands.menu.ProjectAshMenus
import io.github.sirmustfailalot.projectash.commands.player.PlayerCatchEmAll
import io.github.sirmustfailalot.projectash.commands.player.PlayerSpecialChecks
import io.github.sirmustfailalot.projectash.commands.server.*
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ProjectAshCommand {
    private val serverSubs: List<PAServerSubcommand> = listOf(
        ServerDiscord,
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
        val root = literal("ProjectAsh").executes { ctx ->
            if (ctx.source.hasPermission(3)) ProjectAshMenus.root(ctx.source)
            else ProjectAshMenus.player(ctx.source)
            1
        }

        val serverRoot = literal("Server")
            .requires { it.hasPermission(3) }
            .executes { ctx -> ProjectAshMenus.server(ctx.source); 1 }
        serverSubs.forEach { serverRoot.then(it.build()) }
        root.then(serverRoot)

        val playerRoot = literal("Player")
            .executes { ctx -> ProjectAshMenus.player(ctx.source); 1 }
        playerSubs.forEach { playerRoot.then(it.build()) }
        root.then(playerRoot)

        dispatcher.register(root)
    }
}
