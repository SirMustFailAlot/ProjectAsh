package io.github.sirmustfailalot.projectash.commands.player

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAPlayerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.executingPlayerNameOrFail
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object PlayerCatchEmAll : PAPlayerSubcommand {
    private val section = MenuSection(
        "Player CatchEmAll",
        "/ProjectAsh Player CatchEmAll",
        "/ProjectAsh Player",
        listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE)
    )
    private val localSection = MenuSection(
        "Player CatchEmAll > LocalSpawnsOnly",
        "/ProjectAsh Player CatchEmAll LocalSpawnsOnly",
        "/ProjectAsh Player CatchEmAll",
        listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE)
    )

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("CatchEmAll")
            .executes { ctx ->
                ProjectAshMenus.section(
                    ctx.source,
                    section,
                    listOf(MenuButton("LocalSpawnsOnly", "/ProjectAsh Player CatchEmAll LocalSpawnsOnly", colour = ChatFormatting.GOLD))
                )
                1
            }
            .then(checkCommand())
            .then(toggleCommand("Enable", true))
            .then(toggleCommand("Disable", false))
            .then(localSpawnsOnlyCommand())

    private fun checkCommand() = literal("Check").executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        val rule = Config.ensurePlayer(player).catchEmAllMode
        ctx.source.sendSuccess({
            Component.literal("[Project Ash] CatchEmAll: ")
                .append(if (rule.enabled) Component.literal("ENABLED").withStyle(ChatFormatting.GREEN) else Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                .append(Component.literal(" | LocalSpawnsOnly: "))
                .append(if (rule.localSpawnsOnly) Component.literal("ENABLED").withStyle(ChatFormatting.GREEN) else Component.literal("DISABLED").withStyle(ChatFormatting.RED))
        }, false)
        1
    }

    private fun toggleCommand(name: String, enabled: Boolean) = literal(name).executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        Config.toggleCatchEmAllModeEnabled(player, enabled)
        ctx.source.sendSuccess({ prefixedStatus("CatchEmAll", enabled) }, false)
        1
    }

    private fun localSpawnsOnlyCommand() = literal("LocalSpawnsOnly")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, localSection); 1 }
        .then(literal("Check").executes { ctx ->
            val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
            val enabled = Config.ensurePlayer(player).catchEmAllMode.localSpawnsOnly
            ctx.source.sendSuccess({ prefixedStatus("CatchEmAll LocalSpawnsOnly", enabled) }, false)
            1
        })
        .then(localToggleCommand("Enable", true))
        .then(localToggleCommand("Disable", false))

    private fun localToggleCommand(name: String, enabled: Boolean) = literal(name).executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        Config.toggleCatchEmAllModeLocal(player, enabled)
        ctx.source.sendSuccess({ prefixedStatus("CatchEmAll LocalSpawnsOnly", enabled) }, false)
        1
    }
}
