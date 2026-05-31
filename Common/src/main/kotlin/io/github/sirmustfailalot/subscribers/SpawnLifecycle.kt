package io.github.sirmustfailalot.projectash.subscribers

// Project Ash Classes
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.announcer.spwaningAnnouncer.announceSpawn
import io.github.sirmustfailalot.projectash.pipeline.RuleEngine

// Cobblemon Classes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

// Java Classes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

// General Logger
import org.slf4j.LoggerFactory

object SpawnLifecycle {
    private val tracked = ConcurrentHashMap<UUID, PokeStream.PokemonLifespan>()
    private val scheduler = Executors.newScheduledThreadPool(1)

    private val logger = LoggerFactory.getLogger("ProjectAsh")
    fun onSpawn(
        pokemonEntity: PokemonEntity,
        spawnReason: String = "Unknown"
    ) {
        // Get Pokémon Information
        val pokeGlance = PokeStream.pokeGlance(pokemonEntity, spawnReason)
        pokeGlance.evaluationResult = RuleEngine.evaluateSpawn(pokeGlance)

        // Not a server message, or no players? Bail!
        if ( !pokeGlance.evaluationResult!!.discordCriteria.isServerAllowedSpawn || (!pokeGlance.evaluationResult!!.discordCriteria.isServerMessage && pokeGlance.evaluationResult!!.playerCriteria.isEmpty())) {
            return
        }

        // Track the spawn
        tracked[pokeGlance.uuidPokemon] = pokeGlance

        announceSpawn(
            pokeGlance = pokeGlance,
            announceDetails = pokeGlance.evaluationResult!!
        )

    }
}