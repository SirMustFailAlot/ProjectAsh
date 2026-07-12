package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.CommandSuggestions
import io.github.sirmustfailalot.projectash.commands.utility.neutralMessage
import io.github.sirmustfailalot.projectash.commands.utility.parseShinyFlag
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.config.ShinyFlag
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerBlacklistChecks : PAServerSubcommand {
    private val section = MenuSection("Server Blacklist", "/ProjectAsh Server Blacklist", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ADD, MenuAction.REMOVE, MenuAction.CLEAR))

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("Blacklist")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
        .then(checkCommand())
        .then(clearCommand())
        .then(speciesCommand("Add", false, Config::addServerBlacklistRule))
        .then(speciesCommand("Remove", true, Config::removeServerBlacklistRule))

    private fun checkCommand() = literal("Check").executes { ctx ->
        val rules = Config.getServerBlacklistRules()
        val message = if (rules.isEmpty()) "[Project Ash] Server blacklist: none" else buildString {
            appendLine("[Project Ash] Server blacklist:")
            rules.forEachIndexed { i, rule -> appendLine("${i + 1}. ${rule.speciesName} (${rule.shinyFlag})") }
        }.trimEnd()
        ctx.source.sendSuccess({ Component.literal(message) }, false)
        1
    }

    private fun clearCommand() = literal("Clear").executes { ctx ->
        if (Config.clearServerBlacklistRules()) ctx.source.sendSuccess({ Component.literal("[Project Ash] Server blacklist cleared.") }, true)
        else ctx.source.sendSuccess({ neutralMessage("No server blacklist rules to clear.") }, false)
        1
    }

    private fun speciesCommand(actionName: String, removal: Boolean, action: (String, ShinyFlag) -> Boolean) =
        literal(actionName).then(
            argument("species", StringArgumentType.word())
                .suggests(if (removal) CommandSuggestions.serverBlacklistSpecies else CommandSuggestions.species)
                .executes { ctx -> runAction(ctx, ShinyFlag.INCLUDE, action, actionName) }
                .then(argument("shinyFlag", StringArgumentType.word())
                    .suggests(CommandSuggestions.shinyFlags)
                    .executes { ctx ->
                        val flag = parseShinyFlag(StringArgumentType.getString(ctx, "shinyFlag"))
                        if (flag == null) {
                            ctx.source.sendFailure(Component.literal("[Project Ash] Shiny flag must be Include, Exclude or Only."))
                            return@executes 0
                        }
                        runAction(ctx, flag, action, actionName)
                    })
        )

    private fun runAction(ctx: CommandContext<CommandSourceStack>, flag: ShinyFlag, action: (String, ShinyFlag) -> Boolean, actionName: String): Int {
        val species = StringArgumentType.getString(ctx, "species").lowercase()
        if (action(species, flag)) ctx.source.sendSuccess({ Component.literal("[Project Ash] ${if (actionName == "Add") "Added" else "Removed"} blacklist rule: $species ($flag)") }, true)
        else ctx.source.sendSuccess({ neutralMessage("No change for $species ($flag).") }, false)
        return 1
    }
}
