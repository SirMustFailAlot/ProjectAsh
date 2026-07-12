package io.github.sirmustfailalot.projectash.pipeline

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokedex.Dexes
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.config.ShinyFlag
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers.server
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory

object RuleEngine {

    fun evaluateSpawn(
        pokeGlance: PokeStream.PokemonLifespan
    ): RuleEvaluationResult {
        val logger = LoggerFactory.getLogger("ProjectAsh")
        val result = RuleEvaluationResult()

        val isServerBlacklist = Config.data.server.blacklistCheck.any {
            it.speciesName.equals(pokeGlance.species.replace(" ", "-"), ignoreCase = true) &&
                    shinyFlagMatches(it.shinyFlag, pokeGlance.isShiny)
        }

        if (!isServerBlacklist) {
            // Check Allowed Spawn Types
            result.discordCriteria.isServerAllowedSpawn =
                if (Config.data.server.checkUnknownSpawns && pokeGlance.spawnSource == "Unknown") {
                    true
                } else {
                    pokeGlance.spawnSource == "Known" || pokeGlance.spawnSource == "Egg"
                }

            // Check Shiny Spawns
            if (Config.data.server.shinyCheck && pokeGlance.isShiny) {
                result.discordCriteria.isServerMessage = true
                result.discordCriteria.serverLabels.add("Shiny")
                result.discordCriteria.serverRules.add("Shiny Rule")
            }

            // Check Perfect IVs / Hatches
            if ((pokeGlance.spawnSource == "Egg" || Config.data.server.perfectCheck) && pokeGlance.isPerfectIV) {
                result.discordCriteria.isServerMessage = true
                result.discordCriteria.serverLabels.add("Perfect")
                result.discordCriteria.serverRules.add("Perfect Rule")
            }

            // Check Label Spawns
            val serverLabels = Config.data.server.labelCheck
            val hasServerLabel = serverLabels.firstOrNull {
                it.lowercase() in pokeGlance.hasLabels.lowercase()
            } ?: ""

            if (pokeGlance.spawnSource != "Egg" && hasServerLabel != "") {
                result.discordCriteria.isServerMessage = true
                result.discordCriteria.serverLabels.add(pokeGlance.hasLabels)
                result.discordCriteria.serverRules.add("Label Rule")
            }

            // Check Server Special Spawns
            if (pokeGlance.spawnSource != "Egg") {
                val isServerSpecial = Config.data.server.specialCheck.any {
                    it.speciesName.equals(pokeGlance.species, ignoreCase = true) &&
                            shinyFlagMatches(it.shinyFlag, pokeGlance.isShiny)
                }

                if (isServerSpecial) {
                    result.discordCriteria.isServerMessage = true
                    result.discordCriteria.serverLabels.add("Special")
                    result.discordCriteria.serverRules.add("Special Rule")
                }
            }
        }

        server?.playerList?.players?.forEach { player ->
            val playerName = player.scoreboardName
            val playerNotification = InGamePlayerCriteria()

            // Every player starts with the baseline server labels.
            playerNotification.finalLabels.addAll(result.discordCriteria.serverLabels)

            if (pokeGlance.spawnSource != "Egg") {
                // CatchEmAll check
                val catchEmAllEnabled =
                    Config.data.player[playerName]?.catchEmAllMode?.enabled ?: false

                if (catchEmAllEnabled) {
                    val requiresPokemon = isNewCatch(
                        pokemon = pokeGlance.pokemon,
                        serverPlayer = player
                    )

                    val localSpawnOnly =
                        Config.data.player[playerName]?.catchEmAllMode?.localSpawnsOnly ?: true

                    val requiresSpawn =
                        !localSpawnOnly || pokeGlance.spawnClosestPlayer == playerName

                    if (requiresPokemon && requiresSpawn) {
                        playerNotification.finalLabels.add("CatchEmAll")
                    }
                }

                // Player Special checks
                Config.getPlayerSpecialRules(playerName).forEach {
                    if (
                        pokeGlance.species.equals(it.speciesName, ignoreCase = true) &&
                        shinyFlagMatches(it.shinyFlag, pokeGlance.isShiny)
                    ) {
                        playerNotification.finalLabels.add("Special")
                    }
                }
            }

            if (playerNotification.finalLabels.isNotEmpty()) {
                result.playerCriteria[playerName] = playerNotification
            }
        }

        return result
    }

    fun isNewCatch(
        pokemon: Pokemon,
        serverPlayer: ServerPlayer
    ): Boolean {
        val dex = Cobblemon.playerDataManager.getPokedexData(serverPlayer)

        val speciesRecord = dex.getSpeciesRecord(pokemon.species.resourceIdentifier)
            ?: return true

        val dexEntry = Dexes.dexEntryMap[cobblemonResource("national")]
            ?.getEntries()
            ?.firstOrNull { it.speciesId == pokemon.species.resourceIdentifier }
            ?: return true

        val matchingForm = dexEntry.forms
            .firstOrNull { it.displayForm.equals(pokemon.form.name, ignoreCase = true) }
            ?: return true

        return matchingForm.unlockForms.none { unlockFormKey ->
            val isShinyKey = unlockFormKey.contains("shiny", ignoreCase = true)

            if (pokemon.shiny != isShinyKey) {
                return@none false
            }

            val formRecord = speciesRecord.getFormRecord(unlockFormKey)
                ?: return@none false

            formRecord.knowledge == PokedexEntryProgress.CAUGHT
        }
    }

    private fun shinyFlagMatches(
        shinyFlag: ShinyFlag,
        isShiny: Boolean
    ): Boolean =
        when (shinyFlag) {
            ShinyFlag.INCLUDE -> true
            ShinyFlag.EXCLUDE -> !isShiny
            ShinyFlag.ONLY -> isShiny
        }
}