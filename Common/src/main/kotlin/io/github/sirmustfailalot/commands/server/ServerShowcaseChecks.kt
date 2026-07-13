package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import java.net.URI

object ServerShowcaseChecks : PAServerSubcommand {
    private val section = MenuSection("Server Showcase", "/ProjectAsh Server Showcase", "/ProjectAsh Server", listOf(MenuAction.CHECK))
    private val cobbleTcgSection = MenuSection("Server Showcase > CobbleTCG", "/ProjectAsh Server Showcase CobbleTCG", "/ProjectAsh Server Showcase", listOf(MenuAction.ENABLE, MenuAction.DISABLE))

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("Showcase")
        .executes { ctx ->
            ProjectAshMenus.section(ctx.source, section, listOf(
                MenuButton("CobbleTCG", "/ProjectAsh Server Showcase CobbleTCG", colour = ChatFormatting.BLUE)
            ))
            1
        }
        .then(literal("Check").executes { ctx ->
            ctx.source.sendSuccess({
                Component.literal("[Project Ash] Showcase: ")
                    .append(if (Config.data.server.showcase.tcgEnabled) Component.literal("ENABLED").withStyle(ChatFormatting.GREEN) else Component.literal("DISABLED").withStyle(ChatFormatting.RED))
            }, false)
            1
        })
        .then(cobbleTcgCommand())

    private fun cobbleTcgCommand() = literal("CobbleTCG")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, cobbleTcgSection); 1 }
        .then(literal("Check").executes { ctx -> ctx.source.sendSuccess({ prefixedStatus("Showcase CobbleTCG", Config.data.server.showcase.tcgEnabled) }, false); 1 })
        .then(literal("Enable").executes { ctx -> Config.setServerShowcaseEnabledTCG(true); ctx.source.sendSuccess({ prefixedStatus("Showcase CobbleTCG", true) }, true); 1 })
        .then(literal("Disable").executes { ctx -> Config.setServerShowcaseEnabledTCG(false); ctx.source.sendSuccess({ prefixedStatus("Showcase CobbleTCG", false) }, true); 1 })
}
