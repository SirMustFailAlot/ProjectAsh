package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

import io.github.sirmustfailalot.Config
import io.github.sirmustfailalot.Config.getPlayerSpecialRules
import io.github.sirmustfailalot.Config.addPlayerSpecialRule
import io.github.sirmustfailalot.Config.removePlayerSpecialRule
import net.minecraft.ChatFormatting

object PlayerSpecialChecks : PAPlayerSubcommand {

    /** Species suggestions from sprites (fallback to a tiny list). */
    private val SPECIES_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, b ->
            val species = if (Config.data.sprites.isNotEmpty())
                Config.data.sprites.keys
                    .map { it.substringBefore(':').substringBefore('/').substringBefore('_') }
            else listOf("shuckle", "pikachu", "greninja", "ninetales", "gengar")
            species.distinct().sorted().forEach { b.suggest(it.lowercase()) }
            b.buildFuture()
        }

    /** Booleans with false first (our default). */
    private val BOOL_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, b -> b.suggest("false"); b.suggest("true"); b.buildFuture() }

    /** Helper to get the executing player's name or fail gracefully if not a player. */
    private fun executingPlayerNameOrFail(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>): String? {
        return try {
            ctx.source.playerOrException.scoreboardName
        } catch (_: Exception) {
            ctx.source.sendFailure(Component.literal("[Project Ash] This command must be run by a player in-game."))
            null
        }
    }

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("Special")
            .then(literal("SpecialEnabled")
                .then(literal("enabled")
                .executes { ctx ->
                    val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                    Config.setPlayerSpecialCheck(player, true)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Player Special Checks: ENABLED").withStyle(
                        ChatFormatting.GREEN) }, true)
                    1
                }
                )
                .then(literal("disabled")
                .executes { ctx ->
                    val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                    Config.setPlayerSpecialCheck(player, false)
                    ctx.source.sendSuccess({ Component.literal("[Project Ash] Player Special Checks: DISABLED").withStyle(ChatFormatting.RED) }, true)
                    1
                }
                )
            )
            // /ProjectAsh Player Special Check
            .then(literal("Check")
                .executes { ctx ->
                    val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                    val rules = getPlayerSpecialRules(player)
                    if (rules.isEmpty()) {
                        ctx.source.sendSuccess(
                            { Component.literal("[Project Ash] Player ($player) special targets: (none)") }, true
                        )
                        1
                    } else {
                        val lines = buildString {
                            appendLine("[Project Ash] Player ($player) special targets:")
                            rules.forEachIndexed { i, r ->
                                appendLine("  ${i + 1}. ${r.speciesName}  shinyOnly=${r.shinyCheck}")
                            }
                        }
                        ctx.source.sendSuccess({ Component.literal(lines.trimEnd()) }, true)
                        1
                    }
                }
            )
            // /Projectash Player Special Clear
            .then(literal("Clear")
                .executes { ctx ->
                    val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                    val clearRules = Config.clearPlayerSpecialRules(player)
                    if (clearRules) {
                        ctx.source.sendSuccess({ Component.literal("[Project Ash] Player Specials Cleared!") }, true)
                        1
                    } else {
                        ctx.source.sendFailure(Component.literal("[Project Ash] Player Specials Failed to Clear!"))
                        0
                    }
                })

            // /ProjectAsh Player Special Add <species> [shinyOnly]
            .then(literal("Add")
                .then(argument("species", StringArgumentType.word())
                    .suggests(SPECIES_SUGGESTER)
                    // explicit shinyOnly
                    .then(argument("shinyOnly", BoolArgumentType.bool())
                        .suggests(BOOL_SUGGESTER)
                        .executes { ctx ->
                            val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                            val species = StringArgumentType.getString(ctx, "species")
                            val shinyOnly = BoolArgumentType.getBool(ctx, "shinyOnly")
                            if (addPlayerSpecialRule(player, species = species, shinyOnly)) {
                                ctx.source.sendSuccess({
                                    Component.literal("[Project Ash] Added for $player: ${species.lowercase()}  shinyOnly=$shinyOnly")
                                }, true)
                                1
                            } else {
                                ctx.source.sendFailure(Component.literal("[Project Ash] No change: already present or invalid."))
                                0
                            }
                        }
                    )
                    // default shinyOnly=false when omitted
                    .executes { ctx ->
                        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                        val species = StringArgumentType.getString(ctx, "species")
                        val shinyOnly = false
                        if (addPlayerSpecialRule(player, species = species, shinyOnly)) {
                            ctx.source.sendSuccess({
                                Component.literal("[Project Ash] Added for $player: ${species.lowercase()}  shinyOnly=$shinyOnly (default)")
                            }, true)
                            1
                        } else {
                            ctx.source.sendFailure(Component.literal("[Project Ash] No change: already present or invalid."))
                            0
                        }
                    }
                )
            )
            // /ProjectAsh Player Special Remove <species> [shinyOnly]
            .then(literal("Remove")
                .then(argument("species", StringArgumentType.word())
                    .suggests(SPECIES_SUGGESTER)
                    // explicit shinyOnly
                    .then(argument("shinyOnly", BoolArgumentType.bool())
                        .suggests(BOOL_SUGGESTER)
                        .executes { ctx ->
                            val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                            val species = StringArgumentType.getString(ctx, "species")
                            val shinyOnly = BoolArgumentType.getBool(ctx, "shinyOnly")
                            if (removePlayerSpecialRule(player, species = species, shinyOnly)) {
                                ctx.source.sendSuccess({
                                    Component.literal("[Project Ash] Removed for $player: ${species.lowercase()}  shinyOnly=$shinyOnly")
                                }, true)
                                1
                            } else {
                                ctx.source.sendFailure(Component.literal("[Project Ash] No change: not present or invalid."))
                                0
                            }
                        }
                    )
                    // default shinyOnly=false when omitted
                    .executes { ctx ->
                        val player = executingPlayerNameOrFail(ctx) ?: return@executes 0
                        val species = StringArgumentType.getString(ctx, "species")
                        val shinyOnly = false
                        if (removePlayerSpecialRule(player, species = species, shinyOnly)) {
                            ctx.source.sendSuccess({
                                Component.literal("[Project Ash] Removed for $player: ${species.lowercase()}  shinyOnly=$shinyOnly (default)")
                            }, true)
                            1
                        } else {
                            ctx.source.sendFailure(Component.literal("[Project Ash] No change: not present or invalid."))
                            0
                        }
                    }
                )
            )

}
