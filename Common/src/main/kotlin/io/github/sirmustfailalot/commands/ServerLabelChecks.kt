package io.github.sirmustfailalot.projectash.commands

import io.github.sirmustfailalot.Config
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ServerLabelChecks : PAServerSubcommand {
    private val CANON_LABELS = listOf("legendary", "mythical", "ultra-beast", "paradox")
    private val ADD_LABEL_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            val current = Config.getLabelCheck().map { it.trim().lowercase() }.toSet()
            (CANON_LABELS + CANON_LABELS.filterNot { it in current })
                .distinct()
                .forEach { builder.suggest(it) }
            builder.buildFuture()
        }
    private val REMOVE_LABEL_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            Config.getLabelCheck().forEach { builder.suggest(it) }
            builder.buildFuture()
        }

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        // usage: /projectash server LabelCheck <Check|Add|Remove> [label]
        literal("Label")
            // /... Label Check
            .then(
                literal("Check")
                    .executes { ctx ->
                        val labels = Config.getLabelCheck()
                        val pretty = if (labels.isEmpty()) "(none)" else labels.joinToString(", ")
                        ctx.source.sendSuccess({ Component.literal("[Project Ash] Current Server Labels: $pretty") }, false)
                        1
                    }
            )
            // /... Label Add <label>
            .then(
                literal("Add")
                    .then(
                        argument("label", StringArgumentType.word())
                            .suggests(ADD_LABEL_SUGGESTER)
                            .executes { ctx ->
                                val input = StringArgumentType.getString(ctx, "label")
                                val ok = Config.addLabelCheck(input)
                                if (ok) {
                                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Added label: ${input.trim().lowercase()}") }, true)
                                    1
                                } else {
                                    ctx.source.sendFailure(Component.literal("[Project Ash] No change: empty, duplicate, or invalid."))
                                    0
                                }
                            }
                    )
            )
            // /... Label Remove <label>
            .then(
                literal("Remove")
                    .then(
                        argument("label", StringArgumentType.word())
                            .suggests(REMOVE_LABEL_SUGGESTER)
                            .executes { ctx ->
                                val input = StringArgumentType.getString(ctx, "label")
                                val ok = Config.removeLabelCheck(input)
                                if (ok) {
                                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Removed label: ${input.trim().lowercase()}") }, true)
                                    1
                                } else {
                                    ctx.source.sendFailure(Component.literal("[Project Ash] No change: label not present or invalid."))
                                    0
                                }
                            }
                    )
            )
}
