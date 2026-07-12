package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ServerPerfectChecks : PAServerSubcommand {
    private val section = MenuSection("Server Perfect IV Checks", "/ProjectAsh Server Perfect", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE))
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("Perfect")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ prefixedStatus("Perfect IV checks", Config.data.server.perfectCheck) }, false); 1 })
        .then(literal("Enable").executes { ctx -> Config.setServerPerfectCheck(true); ctx.source.sendSuccess({ prefixedStatus("Perfect IV checks", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setServerPerfectCheck(false); ctx.source.sendSuccess({ prefixedStatus("Perfect IV checks", false) }, true); 1 })
}
