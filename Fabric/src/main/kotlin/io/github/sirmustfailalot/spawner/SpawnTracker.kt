import com.cobblemon.mod.common.api.events.battles.BattleEvent
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonFaintedEvent
import java.lang.ref.WeakReference
import java.util.UUID
import net.minecraft.world.entity.Entity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.toVec3d
import io.github.sirmustfailalot.Announcement
import io.github.sirmustfailalot.Config
import io.github.sirmustfailalot.Discord
import io.github.sirmustfailalot.ProjectAsh
import io.github.sirmustfailalot.utility.PokemonUtility

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.String
import kotlin.collections.contains
import kotlin.text.lowercase
import kotlin.toString

object SpawnTracker {
    private val tracked = ConcurrentHashMap<UUID, PokemonUtility.PokemonLifespan>()
    private val scheduler = Executors.newScheduledThreadPool(1)

    enum class Outcome { CAUGHT, FAINTED, NATURAL_DESPAWN }
    fun onSpawn(pokemonEntity: PokemonEntity, spawnSource: String) {
        if (!Config.data.server.checkUnknownSpawns && spawnSource == "Unknown") return

        val quickGlance = PokemonUtility.quickGlance(pokemonEntity = pokemonEntity, spawnSource = spawnSource)
        tracked[quickGlance.uuidPokemon] = quickGlance

        Announcement.spawn(
            server = ProjectAsh.server,

            // Spawn and Species Information
            spawnSource = quickGlance.spawnSource,
            spawnDimension = quickGlance.spawnDimension,
            spawnPos = quickGlance.spawnPos,
            spawnClosestPlayer = quickGlance.spawnClosestPlayer,
            speciesWithForm = quickGlance.speciesWithForm,
            
            // Pokémon Aspects
            hasLabels = quickGlance.hasLabels,
            isShiny = quickGlance.isShiny,
            
            // Announcement Checks
            isServerAnnouncement = quickGlance.isServerAnnouncement,
            isServerSpecial = quickGlance.isServerSpecial,
            hasServerLabel = quickGlance.hasServerLabel,
            hasSpecialPlayers = quickGlance.hasSpecialPlayers,
            targetSpecialPlayers = quickGlance.targetSpecialPlayers,
            hasCatchEmAllPlayers = quickGlance.hasCatchEmAllPlayers,
            targetCatchEmAllPlayers = quickGlance.targetCatchEmAllPlayers
        )
        Discord.spawn(
            server = ProjectAsh.server,

            // Spawn and Species Information
            spawnSource = quickGlance.spawnSource,
            spawnDimension = quickGlance.spawnDimension,
            spawnPos = quickGlance.spawnPos,
            spawnClosestPlayer = quickGlance.spawnClosestPlayer,
            speciesWithForm = quickGlance.speciesWithForm,

            // Pokémon Aspects
            hasLabels = quickGlance.hasLabels,
            isShiny = quickGlance.isShiny,

            // Announcement Checks
            isServerAnnouncement = quickGlance.isServerAnnouncement,
            isServerSpecial = quickGlance.isServerSpecial,
            hasServerLabel = quickGlance.hasServerLabel,
            thumbnail = quickGlance.thumbnail
        )
    }

//    // ---- captures etc ----
//    fun onCapture(player: ServerPlayer, pokemon: Pokemon) {
//        val t = findTracked(pokemon.uuid) ?: return
//        tracked.remove(pokemon.uuid)
//        if (t.announceTarget == "Server") {
//            Announcement.capture(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, player.gameProfile.name, t.spawntype, t.species)
//            Discord.announcement(eventType="Captured", server=ProjectAsh.server, playerName=player.gameProfile.name, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm, thumbnailURL=t.thumbnailURL)
//        } else if (t.announceTarget == "Players") {
//            Announcement.capture(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, player.gameProfile.name, t.spawntype, t.species)
//        }
//    }
//    fun onFainted(capture: PokemonFaintedEvent) {
//        val t = findTracked(capture.pokemon.uuid) ?: return
//        tracked.remove(capture.pokemon.uuid)
//        if (t.announceTarget == "Server") {
//            Announcement.fainted(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
//            Discord.announcement(eventType="Fainted", server=ProjectAsh.server, spawnType=t.spawntype, species=t.species, speciesPlusForm=t.speciesForm, thumbnailURL=t.thumbnailURL)
//        } else if (t.announceTarget == "Players") {
//            Announcement.fainted(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
//        }
//    }
//    fun onRemoved(entity: PokemonEntity, removalReason: Entity.RemovalReason?) {
//        val pokeUuid = entity.pokemon.uuid
//
//        scheduler.schedule({
//            ProjectAsh.server?.execute {
//                val t = tracked[pokeUuid] ?: return@execute
//
//                if (t.outcome == null) {
//                    tracked.remove(pokeUuid)
//                    if (t.announceTarget == "Server") {
//                        Announcement.removed(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
//                        Discord.announcement(
//                            eventType = "Despawned",
//                            server = ProjectAsh.server,
//                            spawnType = t.spawntype,
//                            species = t.species,
//                            speciesPlusForm = t.speciesForm,
//                            thumbnailURL=t.thumbnailURL
//                        )
//                    } else if (t.announceTarget == "Players") {
//                        Announcement.removed(announceTarget = t.announceTarget, announcePlayers = t.announcePlayers, ProjectAsh.server, t.spawntype, t.species)
//                    }
//                }
//            }
//        }, 3, TimeUnit.SECONDS)
//    }

//    // ---- helpers ----
//    private fun findTracked(pokemonUuid: UUID): Tracked? {
//        return tracked[pokemonUuid]?.takeIf { it.outcome == null }
//    }


}
