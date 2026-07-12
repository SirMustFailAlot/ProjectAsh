package io.github.sirmustfailalot.projectash.commands.player

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.sirmustfailalot.projectash.commands.PAPlayerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.CommandSuggestions
import io.github.sirmustfailalot.projectash.commands.utility.executingPlayerNameOrFail
import io.github.sirmustfailalot.projectash.commands.utility.parseShinyFlag
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.commands.utility.neutralMessage
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.config.ShinyFlag
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object PlayerSpecialChecks : PAPlayerSubcommand {
    private val section = MenuSection(
        "Player Special",
        "/ProjectAsh Player Special",
        "/ProjectAsh Player",
        listOf(MenuAction.CHECK, MenuAction.ADD, MenuAction.REMOVE, MenuAction.CLEAR, MenuAction.ENABLE, MenuAction.DISABLE)
    )

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("Special")
            .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
            .then(checkCommand())
            .then(clearCommand())
            .then(toggleCommand("Enable", true))
            .then(toggleCommand("Disable", false))
            .then(speciesCommand("Add", false, Config::addPlayerSpecialRule))
            .then(speciesCommand("Remove", true, Config::removePlayerSpecialRule))

    private fun checkCommand() = literal("Check").executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        val rules = Config.getPlayerSpecialRules(player)
        val message = if (rules.isEmpty()) {
            "[Project Ash] Player special targets: none"
        } else buildString {
            appendLine("[Project Ash] Player special targets:")
            rules.forEachIndexed { index, rule -> appendLine("${index + 1}. ${rule.speciesName} (${rule.shinyFlag})") }
        }.trimEnd()
        ctx.source.sendSuccess({ Component.literal(message) }, false)
        1
    }

    private fun clearCommand() = literal("Clear").executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        if (Config.clearPlayerSpecialRules(player)) {
            ctx.source.sendSuccess({ Component.literal("[Project Ash] Player specials cleared.") }, false)
        } else {
            ctx.source.sendSuccess({ neutralMessage("No player specials to clear.") }, false)
        }
        1
    }

    private fun toggleCommand(name: String, enabled: Boolean) = literal(name).executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        Config.setPlayerSpecialCheck(player, enabled)
        ctx.source.sendSuccess({ prefixedStatus("Player special checks", enabled) }, false)
        1
    }

    private fun speciesCommand(
        actionName: String,
        removal: Boolean,
        action: (String, String, ShinyFlag) -> Boolean
    ): LiteralArgumentBuilder<CommandSourceStack> = literal(actionName).then(
        argument("species", StringArgumentType.word())
            .suggests(if (removal) CommandSuggestions.playerSpecialSpecies else CommandSuggestions.species)
            .executes { ctx -> runAction(ctx, ShinyFlag.INCLUDE, action, actionName) }
            .then(
                argument("shinyFlag", StringArgumentType.word())
                    .suggests(CommandSuggestions.shinyFlags)
                    .executes { ctx ->
                        val flag = parseShinyFlag(StringArgumentType.getString(ctx, "shinyFlag"))
                        if (flag == null) {
                            ctx.source.sendFailure(Component.literal("[Project Ash] Shiny flag must be Include, Exclude or Only."))
                            return@executes 0
                        }
                        runAction(ctx, flag, action, actionName)
                    }
            )
    )

    private fun runAction(
        ctx: CommandContext<CommandSourceStack>,
        flag: ShinyFlag,
        action: (String, String, ShinyFlag) -> Boolean,
        actionName: String
    ): Int {
        val player = executingPlayerNameOrFail(ctx) ?: return 0
        val species = StringArgumentType.getString(ctx, "species").lowercase()
        val changed = action(player, species, flag)
        if (changed) {
            ctx.source.sendSuccess({ Component.literal("[Project Ash] ${if (actionName == "Add") "Added" else "Removed"} player special: $species ($flag)") }, false)
        } else {
            ctx.source.sendSuccess({ neutralMessage("No change for $species ($flag).") }, false)
        }
        return 1
    }
}
