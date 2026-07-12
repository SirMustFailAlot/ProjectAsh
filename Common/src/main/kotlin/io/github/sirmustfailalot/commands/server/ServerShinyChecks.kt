package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ServerShinyChecks : PAServerSubcommand {
    private val section = MenuSection("Server Shiny Checks", "/ProjectAsh Server Shiny", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE))
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("Shiny")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ prefixedStatus("Shiny checks", Config.data.server.shinyCheck) }, false); 1 })
        .then(literal("Enable").executes { ctx -> Config.setServerShinyCheck(true); ctx.source.sendSuccess({ prefixedStatus("Shiny checks", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setServerShinyCheck(false); ctx.source.sendSuccess({ prefixedStatus("Shiny checks", false) }, true); 1 })
}
