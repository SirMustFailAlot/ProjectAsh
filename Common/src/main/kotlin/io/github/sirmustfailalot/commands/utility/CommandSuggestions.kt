package io.github.sirmustfailalot.projectash.commands.utility

import com.mojang.brigadier.suggestion.SuggestionProvider
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.commands.CommandSourceStack

object CommandSuggestions {
    private val fallbackSpecies = listOf("shuckle", "pikachu", "greninja", "ninetales", "gengar")
    private val knownLabels = listOf("Legendary", "Ultra Beast", "Mythical", "Paradox")

    val species: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        allSpecies().forEach(builder::suggest)
        builder.buildFuture()
    }

    val serverSpecialSpecies: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        Config.getServerSpecialRules().map { it.speciesName }.distinct().sorted().forEach(builder::suggest)
        builder.buildFuture()
    }

    val serverBlacklistSpecies: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        Config.getServerBlacklistRules().map { it.speciesName }.distinct().sorted().forEach(builder::suggest)
        builder.buildFuture()
    }

    val playerSpecialSpecies: SuggestionProvider<CommandSourceStack> = SuggestionProvider { ctx, builder ->
        val playerName = runCatching { ctx.source.playerOrException.scoreboardName }.getOrNull()
        if (playerName != null) {
            Config.getPlayerSpecialRules(playerName)
                .map { it.speciesName }
                .distinct()
                .sorted()
                .forEach(builder::suggest)
        }
        builder.buildFuture()
    }

    val shinyFlags: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        listOf("Include", "Exclude", "Only").forEach(builder::suggest)
        builder.buildFuture()
    }

    val labelsToAdd: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        val configured = Config.getLabelCheck().map { it.lowercase() }.toSet()
        knownLabels.filterNot { it.lowercase() in configured }.forEach(builder::suggest)
        builder.buildFuture()
    }

    val labelsToRemove: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        Config.getLabelCheck().sortedBy { it.lowercase() }.forEach(builder::suggest)
        builder.buildFuture()
    }

    private fun allSpecies(): List<String> {
        val values = Config.data.sprites.keys.map {
            it.substringBefore(':').substringBefore('/').substringBefore('_').lowercase()
        }
        return (if (values.isEmpty()) fallbackSpecies else values).distinct().sorted()
    }

    val CatchEmAllModes: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        listOf("Disabled", "LivingDex", "ShinyDex", "EveryDex", "FormDex", "MasterLivingDex").forEach(builder::suggest)
        builder.buildFuture()
    }

}
