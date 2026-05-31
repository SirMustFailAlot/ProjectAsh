package io.github.sirmustfailalot.projectash.subscribers

// Project Ash Classes
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.announcer.SpwaningAnnouncer
import io.github.sirmustfailalot.projectash.pipeline.RuleEngine

// Cobblemon Classes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon

// Minecraft Classes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity

// Java Classes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

// General Logger
import org.slf4j.LoggerFactory

object SpawnLifecycle {
    private val pokeSpan = ConcurrentHashMap<UUID, PokeStream.PokemonLifespan>()
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
        pokeSpan[pokeGlance.uuidPokemon] = pokeGlance

        SpwaningAnnouncer.announceSpawn(
            pokeGlance = pokeGlance,
            announceDetails = pokeGlance.evaluationResult!!
        )

    }

    fun onCapture(
        serverPlayer: ServerPlayer,
        pokemon: Pokemon
        ) {
        val pokeGlance = pokeSpan[pokemon.uuid] ?: return     // return if not tracked, means we don't want it.
        val caughtBy = serverPlayer.scoreboardName.toString()
        SpwaningAnnouncer.announceCapture(
            caughtBy = caughtBy,
            pokeGlance = pokeGlance,
            announceDetails = pokeGlance.evaluationResult!!
        )
        pokeSpan.remove(pokemon.uuid)
    }

    fun onFainted(
        pokemon: Pokemon
    ) {
        val pokeGlance = pokeSpan[pokemon.uuid] ?: return     // return if not tracked, means we don't want it.
        SpwaningAnnouncer.announceFainted(
            pokeGlance = pokeGlance,
            announceDetails = pokeGlance.evaluationResult!!
        )
        pokeSpan.remove(pokemon.uuid)
    }

    fun onRemoved(
        pokemonEntity: PokemonEntity,
        removalReason: Entity.RemovalReason?
    ) {
        val pokemon = pokemonEntity.pokemon
        val pokeGlance = pokeSpan[pokemon.uuid] ?: return     // return if not tracked, means we don't want it.
        SpwaningAnnouncer.announceRemoved(
            removalReason = removalReason?.toString() ?: "Unknown",
            pokeGlance = pokeGlance,
            announceDetails = pokeGlance.evaluationResult!!
        )
        pokeSpan.remove(pokemon.uuid)
    }
}