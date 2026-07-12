package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object ServerCheckUnknownSpawns : PAServerSubcommand {
    private val section = MenuSection("Server Unknown Spawns", "/ProjectAsh Server UnknownSpawns", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ENABLE, MenuAction.DISABLE))
    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("UnknownSpawns")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ prefixedStatus("Check unknown spawns", Config.data.server.checkUnknownSpawns) }, false); 1 })
        .then(literal("Enable").executes { ctx -> Config.setCheckUnknownSpawns(true); ctx.source.sendSuccess({ prefixedStatus("Check unknown spawns", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setCheckUnknownSpawns(false); ctx.source.sendSuccess({ prefixedStatus("Check unknown spawns", false) }, true); 1 })
}
