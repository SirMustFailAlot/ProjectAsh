package io.github.sirmustfailalot.projectash.pipeline

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokedex.Dexes
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import io.github.sirmustfailalot.projectash.config.CatchEmAllType
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.config.ShinyFlag
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers.server
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory

val formDexBlacklist = setOf(
    ResourceLocation.fromNamespaceAndPath("cobblemon", "spinda"),
    ResourceLocation.fromNamespaceAndPath("cobblemon", "vivillon"),
    // Add any others if needed, e.g., "alcremie" (depending on how difficult you want it)
)

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
                    Config.data.player[playerName]?.catchEmAllMode?.type ?: false

                if (catchEmAllEnabled != CatchEmAllType.DISABLED) {
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
        val playerName = serverPlayer.scoreboardName
        val dexType = Config.data.player[playerName]?.catchEmAllMode?.type
        val playerParty = Cobblemon.storage.getParty(serverPlayer)
        val playerPC = Cobblemon.storage.getPC(serverPlayer)

        val alreadyHave = when (dexType) {
                CatchEmAllType.LIVINGDEX -> livingDexLogic(pokemon, playerParty, playerPC)
                CatchEmAllType.SHINYDEX -> if (pokemon.shiny) shinyDexLogic(pokemon, playerParty, playerPC) else true
                CatchEmAllType.EVERYDEX -> everyDexLogic(serverPlayer, pokemon)
                CatchEmAllType.FORMDEX -> formDexLogic(pokemon, playerParty, playerPC)
                CatchEmAllType.MASTERLIVINGDEX -> masterDexLogic(pokemon, playerParty, playerPC)
                else -> true
            }

        return !alreadyHave
    }

    fun livingDexLogic(
        pokemon: Pokemon,
        playerParty: PlayerPartyStore,
        playerPC: PCStore
    ): Boolean {
        val inParty = playerParty.any { it.species == pokemon.species }
        if (inParty) return true

        return playerPC.any { it.species == pokemon.species }
    }

    fun shinyDexLogic(
        pokemon: Pokemon,
        playerParty: PlayerPartyStore,
        playerPC: PCStore
    ): Boolean {
        val targetSpeciesId = pokemon.species.resourceIdentifier
        val isShinyMatch = { pcPokemon: Pokemon ->
            pcPokemon.species.resourceIdentifier == targetSpeciesId && pcPokemon.shiny
        }
        if (playerParty.any(isShinyMatch)) return true
        return playerPC.any(isShinyMatch)
    }

    fun everyDexLogic(player: ServerPlayer, targetPokemon: Pokemon): Boolean {
        val dex = Cobblemon.playerDataManager.getPokedexData(player)
        val speciesRecord = dex.getSpeciesRecord(targetPokemon.species.resourceIdentifier)
            ?: return false
        val dexEntry = Dexes.dexEntryMap[cobblemonResource("national")]
            ?.getEntries()
            ?.firstOrNull { it.speciesId == targetPokemon.species.resourceIdentifier }
            ?: return false
        val allUnlockKeys = dexEntry.forms.flatMap { it.unlockForms }
        return allUnlockKeys.any { unlockFormKey ->
            val formRecord = speciesRecord.getFormRecord(unlockFormKey)
            formRecord != null && formRecord.knowledge == PokedexEntryProgress.CAUGHT
        }
    }

    fun formDexLogic(
         pokemon: Pokemon,
         playerParty: PlayerPartyStore,
         playerPC: PCStore
    ): Boolean {
        val targetSpeciesId = pokemon.species.resourceIdentifier
        val bypassFormCheck = formDexBlacklist.contains(targetSpeciesId)
        val isMatch = { pcPokemon: Pokemon ->
            pcPokemon.species.resourceIdentifier == targetSpeciesId && (
                    bypassFormCheck ||
                            pcPokemon.form.name.equals(pokemon.form.name, ignoreCase = true)
                    )
        }
        if (playerParty.any(isMatch)) return true
        return playerPC.any(isMatch)
    }

    fun masterDexLogic(
        pokemon: Pokemon,
        playerParty: PlayerPartyStore,
        playerPC: PCStore
    ): Boolean {
        val targetSpeciesId = pokemon.species.resourceIdentifier
        val targetFormName = pokemon.form.name
        val bypassFormCheck = formDexBlacklist.contains(targetSpeciesId)

        val isExactMatch = { pcPokemon: Pokemon ->
            pcPokemon.species.resourceIdentifier == targetSpeciesId &&
                    pcPokemon.shiny == pokemon.shiny && (
                    bypassFormCheck ||
                            pcPokemon.form.name.equals(targetFormName, ignoreCase = true)
                    )
        }

        val alreadyOwnsVariant = playerParty.any(isExactMatch) || playerPC.any(isExactMatch)

        return alreadyOwnsVariant
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