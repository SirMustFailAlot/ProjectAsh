package io.github.sirmustfailalot.projectash.commands.player

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.sirmustfailalot.projectash.commands.PAPlayerSubcommand
import io.github.sirmustfailalot.projectash.commands.menu.*
import io.github.sirmustfailalot.projectash.commands.utility.executingPlayerNameOrFail
import io.github.sirmustfailalot.projectash.commands.utility.prefixedStatus
import io.github.sirmustfailalot.projectash.config.CatchEmAllType
import io.github.sirmustfailalot.projectash.commands.utility.parseCatchEmAllFlag
import io.github.sirmustfailalot.projectash.commands.utility.CommandSuggestions
import io.github.sirmustfailalot.projectash.commands.utility.neutralMessage
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import net.minecraft.commands.Commands.argument

object PlayerCatchEmAll : PAPlayerSubcommand {
    private val section = MenuSection(
        "Player CatchEmAll",
        "/ProjectAsh Player CatchEmAll",
        "/ProjectAsh Player",
        listOf(MenuAction.CHECK, MenuAction.UPDATE)
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
            .then(catchEmAllTypeCommand("Update", Config::toggleCatchEmAllModeEnabled))
            .then(localSpawnsOnlyCommand())

    private fun checkCommand() = literal("Check").executes { ctx ->
        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
        val rule = Config.ensurePlayer(player).catchEmAllMode
        ctx.source.sendSuccess({
            Component.literal("[Project Ash] CatchEmAll: ")
                .append(if (rule.type.toString() == "DISABLED") Component.literal("DISABLED").withStyle(ChatFormatting.RED) else
                            if (rule.type.toString() == "LIVINGDEX") Component.literal("LIVINGDEX").withStyle(ChatFormatting.GREEN) else
                                if (rule.type.toString() == "SHINYDEX") Component.literal("SHINYDEX").withStyle(ChatFormatting.GOLD) else
                                    if (rule.type.toString() == "EVERYDEX") Component.literal("EVERYDEX").withStyle(ChatFormatting.DARK_RED) else
                                        if (rule.type.toString() == "FORMDEX") Component.literal("FORMDEX").withStyle(ChatFormatting.BLUE) else
                                            if (rule.type.toString() == "MASTERLIVINGDEX") Component.literal("MASTERLIVINGDEX").withStyle(ChatFormatting.DARK_PURPLE) else Component.literal(""))
                .append(Component.literal(" | LocalSpawnsOnly: "))
                .append(if (rule.localSpawnsOnly) Component.literal("ENABLED").withStyle(ChatFormatting.GREEN) else Component.literal("DISABLED").withStyle(ChatFormatting.RED))
        }, false)
        1
    }

    private fun catchEmAllTypeCommand(
        actionName: String,
        action: (String, CatchEmAllType) -> Boolean
    ): LiteralArgumentBuilder<CommandSourceStack> = literal(actionName).then(
        argument("type", StringArgumentType.word())
            .suggests(CommandSuggestions.CatchEmAllModes)
            .executes { ctx ->
                val flag = parseCatchEmAllFlag(StringArgumentType.getString(ctx, "type"))
                if (flag == null) {
                    ctx.source.sendFailure(Component.literal("[Project Ash] CatchEmAll Type must be Disabled, LivingDex, ShinyDex, EveryDex, or FormDex."))
                    return@executes 0
                }
                runAction(ctx, flag as CatchEmAllType, action, actionName)
            }
    )

    private fun runAction(
        ctx: CommandContext<CommandSourceStack>,
        flag: CatchEmAllType,
        action: (String, CatchEmAllType) -> Boolean,
        actionName: String
    ): Int {
        val player = executingPlayerNameOrFail(ctx) ?: return 0
        val changed = action(player, flag)
        if (changed) {
            val successMessage = Component.literal("[Project Ash] CatchEmAll has been updated to ")
                .append(when (flag.toString()) {
                    "DISABLED" -> Component.literal("DISABLED").withStyle(ChatFormatting.RED)
                    "LIVINGDEX" -> Component.literal("LIVINGDEX").withStyle(ChatFormatting.GREEN)
                    "SHINYDEX" -> Component.literal("SHINYDEX").withStyle(ChatFormatting.GOLD)
                    "EVERYDEX" -> Component.literal("EVERYDEX").withStyle(ChatFormatting.DARK_RED)
                    "FORMDEX" -> Component.literal("FORMDEX").withStyle(ChatFormatting.BLUE)
                    "MASTERLIVINGDEX" -> Component.literal("MASTERLIVINGDEX").withStyle(ChatFormatting.DARK_PURPLE)
                    else -> Component.literal("")

                })
            ctx.source.sendSuccess({ successMessage }, false)
        } else {
            ctx.source.sendSuccess({ neutralMessage("No change for CatchEmAll Mode ($flag).") }, false)
        }
        return 1
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
