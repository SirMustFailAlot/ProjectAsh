package io.github.sirmustfailalot.projectash.commands.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.sirmustfailalot.projectash.commands.PAServerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.CommandSuggestions
import io.github.sirmustfailalot.projectash.commands.utility.neutralMessage
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerLabelChecks : PAServerSubcommand {
    private val section = MenuSection("Server Label Checks", "/ProjectAsh Server Label", "/ProjectAsh Server", listOf(MenuAction.CHECK, MenuAction.ADD, MenuAction.REMOVE, MenuAction.CLEAR))

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> = literal("Label")
        .executes { ctx -> ProjectAshMenus.section(ctx.source, section); 1 }
        .then(checkCommand())
        .then(addCommand())
        .then(removeCommand())
        .then(clearCommand())

    private fun checkCommand() = literal("Check").executes { ctx ->
        val labels = Config.getLabelCheck()
        val message = if (labels.isEmpty()) "[Project Ash] Server label checks: none" else "[Project Ash] Server label checks: ${labels.joinToString(", ")}"
        ctx.source.sendSuccess({ Component.literal(message) }, false)
        1
    }

    private fun addCommand() = literal("Add").then(
        argument("label", StringArgumentType.greedyString())
            .suggests(CommandSuggestions.labelsToAdd)
            .executes { ctx ->
                val label = StringArgumentType.getString(ctx, "label").trim()
                if (Config.addLabelCheck(label)) ctx.source.sendSuccess({ Component.literal("[Project Ash] Added label check: $label") }, true)
                else ctx.source.sendSuccess({ neutralMessage("Label is invalid or already configured: $label") }, false)
                1
            }
    )

    private fun removeCommand() = literal("Remove").then(
        argument("label", StringArgumentType.greedyString())
            .suggests(CommandSuggestions.labelsToRemove)
            .executes { ctx ->
                val label = StringArgumentType.getString(ctx, "label").trim()
                if (Config.removeLabelCheck(label)) ctx.source.sendSuccess({ Component.literal("[Project Ash] Removed label check: $label") }, true)
                else ctx.source.sendSuccess({ neutralMessage("Label is not configured: $label") }, false)
                1
            }
    )

    private fun clearCommand() = literal("Clear").executes { ctx ->
        if (Config.clearLabelChecks()) ctx.source.sendSuccess({ Component.literal("[Project Ash] Server label checks cleared.") }, true)
        else ctx.source.sendSuccess({ neutralMessage("No server label checks to clear.") }, false)
        1
    }
}
