package io.github.sirmustfailalot.projectash.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.github.sirmustfailalot.projectash.config.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.Commands.argument
import net.minecraft.network.chat.Component

object ServerBlacklistChecks : PAServerSubcommand {
    // Suggest all species; prefer pulling from sprites for UX alignment.
    private val SPECIES_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            val species = if (Config.data.sprites.isNotEmpty())
                Config.data.sprites.keys
                    .map { it.substringBefore(':').substringBefore('/').substringBefore('_') }
            else
                listOf("shuckle")

            species.distinct().sorted().forEach { builder.suggest(it.lowercase()) }
            builder.buildFuture()
        }

    // Suggest booleans with 'false' first (since it’s our default)
    private val BOOL_SUGGESTER: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, b -> b.suggest("false"); b.suggest("true"); b.buildFuture() }

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        literal("Blacklist")
            // /ProjectAsh Server Special Check
            .then(literal("Check").executes { ctx ->
                val rules = Config.getServerBlacklistRules()
                if (rules.isEmpty()) {
                    ctx.source.sendSuccess({ Component.literal("Server blacklist targets: (none)") }, false)
                    1
                } else {
                    val lines = buildString {
                        appendLine("Server blacklist targets:")
                        rules.forEachIndexed { i, r ->
                            appendLine("  ${i + 1}. ${r.speciesName} includeShiny=${r.shinyCheck}")
                        }
                    }
                    ctx.source.sendSuccess({ Component.literal(lines.trimEnd()) }, false)
                    1
                }
            })
            // /Projectash Server Blacklist Clear
            .then(literal("Clear")
                .executes { ctx ->
                    val clearRules = Config.clearServerBlacklistRules()
                    if (clearRules) {
                        ctx.source.sendSuccess({ Component.literal("[Project Ash] Server Blacklist Cleared!") }, true)
                        1
                    } else {
                        ctx.source.sendFailure(Component.literal("[Project Ash] Server Blacklist Failed to Clear!"))
                        0
                    }
                })
            // /ProjectAsh Server Special Add <species> [shinyOnly]
            .then(literal("Add")
                .then(argument("species", StringArgumentType.word())
                    .suggests(SPECIES_SUGGESTER)
                    .then(argument("includeShiny", BoolArgumentType.bool())
                        .suggests(BOOL_SUGGESTER)
                        .executes { ctx ->
                            val species = StringArgumentType.getString(ctx, "species")
                            val shinyOnly = BoolArgumentType.getBool(ctx, "includeShiny")
                            if (Config.addServerBlacklistRule(species, shinyOnly)) {
                                ctx.source.sendSuccess({
                                    Component.literal("Added: ${species.lowercase()}  includeShiny=$shinyOnly")
                                }, true)
                                1
                            } else {
                                ctx.source.sendFailure(Component.literal("No change: already present or invalid."))
                                0
                            }
                        }))
                .executes { ctx ->
                    val species = StringArgumentType.getString(ctx, "species")
                    val shinyOnly = false
                    if (Config.addServerBlacklistRule(species, shinyOnly)) {
                        ctx.source.sendSuccess({
                            Component.literal("Added: ${species.lowercase()}  includeShiny=$shinyOnly (default)")
                        }, true)
                        1
                    } else {
                        ctx.source.sendFailure(Component.literal("No change: already present or invalid."))
                        0
                    }
                }
            )
            // /ProjectAsh Server Special Remove <species> [shinyOnly]
            .then(literal("Remove")
                .then(argument("species", StringArgumentType.word())
                    .suggests(SPECIES_SUGGESTER)
                    // Explicit shinyOnly
                    .then(argument("includeShiny", BoolArgumentType.bool())
                        .suggests(BOOL_SUGGESTER)
                        .executes { ctx ->
                            val species = StringArgumentType.getString(ctx, "species")
                            val shinyOnly = BoolArgumentType.getBool(ctx, "includeShiny")
                            if (Config.removeServerBlacklistRule(species, shinyOnly)) {
                                ctx.source.sendSuccess({
                                    Component.literal("Removed: ${species.lowercase()}  includeShiny=$shinyOnly")
                                }, true)
                                1
                            } else {
                                ctx.source.sendFailure(Component.literal("No change: not present or invalid."))
                                0
                            }
                        }))
                // Default shinyOnly=false if omitted
                .executes { ctx ->
                    val species = StringArgumentType.getString(ctx, "species")
                    val shinyOnly = false
                    if (Config.removeServerBlacklistRule(species, shinyOnly)) {
                        ctx.source.sendSuccess({
                            Component.literal("Removed: ${species.lowercase()}  shinyOnly=$shinyOnly (default)")
                        }, true)
                        1
                    } else {
                        ctx.source.sendFailure(Component.literal("No change: not present or invalid."))
                        0
                    }
                }
            )
}