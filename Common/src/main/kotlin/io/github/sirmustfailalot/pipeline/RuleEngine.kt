package io.github.sirmustfailalot.projectash.pipeline

// Project Ash Classes
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers.server

object RuleEngine {
    fun evaluateSpawn(
        pokeGlance: PokeStream.PokemonLifespan
        ): RuleEvaluationResult {
        val result = RuleEvaluationResult()

        // SERVER RULES
        // Check Allowed Spawn Types
        if (Config.data.server.checkUnknownSpawns && pokeGlance.spawnSource == "Unknown") {
            result.discordCriteria.isServerAllowedSpawn = true
        } else if (pokeGlance.spawnSource == "Known") {
            result.discordCriteria.isServerAllowedSpawn = true
        } else {
            result.discordCriteria.isServerAllowedSpawn = false
        }

        // Check Shiny Spawns
        if (Config.data.server.shinyCheck && pokeGlance.isShiny) {
            result.discordCriteria.isServerMessage = true
            result.discordCriteria.serverLabels.add("Shiny")
            result.discordCriteria.serverRules.add("Shiny Rule")
        }

        // Check Label Spawns
        val serverLabels = Config.data.server.labelCheck
        val hasServerLabel = pokeGlance.hasLabels
        if ( hasServerLabel != "" ) {
            result.discordCriteria.isServerMessage = true
            result.discordCriteria.serverLabels.add(hasServerLabel.replace(Regex("(?<=\\b|\\P{L})\\p{L}")) { matchResult ->
                matchResult.value.uppercase()
            })
            result.discordCriteria.serverRules.add("Label Rule")
        }

        server!!.playerList.players.forEach { player ->
            val playerName = player.scoreboardName.lowercase()
            val playerNotification = InGamePlayerCriteria()

            // Every player automatically starts with the baseline server labels
            playerNotification.finalLabels.addAll(result.discordCriteria.serverLabels)

            // Check Criterion A: Pokedex "Catch 'Em All" check
//            if (Config.data.player.catchEmAllEnabled) {
//                val hasCaught = PokedexUtility.hasPlayerCaught(player, pokemon.species.name)
//                if (!hasCaught) {
//                    playerNotification.finalLabels.add("CatchEmAll")
//                }
//            }

            // Check Criterion B: Player's Special Watchlist list
//            if (Config.data.player.specialListEnabled) {
//                val isOnWatchlist = WatchlistUtility.isPokemonOnPlayerList(player, pokemon.species.name)
//                if (isOnWatchlist) {
//                    playerNotification.finalLabels.add("Watchlist")
//                }
//            }

            if (playerNotification.finalLabels.isNotEmpty()) {
                result.playerCriteria[playerName] = playerNotification
            }
        }
        return result
    }

}