package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ServerInGameEnabled : PAServerSubcommand {
    private val section = MenuSection("Server InGame Announcements", "/ProjectAsh Server InGame", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE))
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("InGame")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ prefixedStatus("In-game announcements", Config.data.server.ingameEnabled) }, false); 1 })
        .then(literal("Enable").executes { ctx -> Config.setServerIngameEnabled(true); ctx.source.sendSuccess({ prefixedStatus("In-game announcements", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setServerIngameEnabled(false); ctx.source.sendSuccess({ prefixedStatus("In-game announcements", false) }, true); 1 })
}
