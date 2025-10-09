package io.github.sirmustfailalot

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import java.io.File

// ── Types ─────────────────────────────────────────────────────────────────────
data class DiscordConfig(
    var enabled: Boolean = true,
    var webhook: String = "https://your.webhook.url/here",
    var thumbnails: Boolean = true
)

data class InGameConfig(
    var enabled: Boolean = true,
    var broadcast_login: Boolean = false,
    var broadcast_logout: Boolean = false
)

data class SpriteEntry(
    var standard: String = "",
    var shiny: String = ""
)

/** Per-player rule: enabled + allowed species list */
data class PlayerRule(
    var enabled: Boolean = false,
    var species: MutableList<String> = mutableListOf()
)

data class ConfigData(
    var discord: DiscordConfig = DiscordConfig(),
    var in_game: InGameConfig = InGameConfig(),
    var player: MutableMap<String, PlayerRule> = mutableMapOf(),           // 👈 now a map
    var sprites: MutableMap<String, SpriteEntry> = mutableMapOf()
)

// ── Config ────────────────────────────────────────────────────────────────────
object Config {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val file = File("config/ProjectAsh.json")

    @Volatile
    var data: ConfigData = ConfigData()
        private set

    /** Call once at startup. Creates defaults (incl. sprites from resource) if missing. */
    fun init() {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            data = ConfigData()
            val defaults = loadSpritesFromResource()
            data.sprites.putAll(defaults)
            save()
        } else {
            reload()
        }
    }

    /** Atomically modify & auto-save. */
    @Synchronized
    fun write(modify: (ConfigData) -> Unit) {
        modify(data)
        saveLocked()
    }

    /** Save current in-memory config. */
    @Synchronized
    fun save() = saveLocked()

    /** Reload from disk (no backfill). */
    @Synchronized
    fun reload() {
        val text = runCatching { file.readText() }.getOrElse {
            logger.info("Project Ash: failed to read config; using defaults: ${it.message}")
            ""
        }
        data = runCatching { gson.fromJson(text, ConfigData::class.java) }
            .getOrElse {
                logger.info("Project Ash: failed to parse config; using defaults: ${it.message}")
                ConfigData()
            }
    }

    /** Optional: reset to defaults and reapply resource sprites. */
    @Synchronized
    fun resetToDefaults() {
        data = ConfigData()
        val defaults = loadSpritesFromResource()
        if (defaults.isNotEmpty()) data.sprites.putAll(defaults)
        saveLocked()
    }

    // ── Convenience helpers ────────────────────────────────────────
    fun setDiscordWebhook(url: String) = write { it.discord.webhook = url }
    fun setDiscordEnabled(enabled: Boolean) = write { it.discord.enabled = enabled }
    fun setDiscordThumbnails(enabled: Boolean) = write { it.discord.thumbnails = enabled }

    fun setIngameEnabled(enabled: Boolean) = write { it.in_game.enabled = enabled }

    /** Ensure a player record exists and return it (in-memory). */
    fun ensurePlayer(name: String): PlayerRule {
        var rule = data.player[name]
        if (rule == null) {
            rule = PlayerRule()
            data.player[name] = rule
            save() // persist new player entry
        }
        return rule
    }

    fun setPlayerEnabled(name: String, enabled: Boolean) = write {
        val rule = it.player.getOrPut(name) { PlayerRule() }
        rule.enabled = enabled
    }

    fun setPlayerSpecies(name: String, speciesList: List<String>) = write {
        val rule = it.player.getOrPut(name) { PlayerRule() }
        rule.species.clear()
        rule.species.addAll(speciesList)
    }

    fun addPlayerSpecies(name: String, species: String) = write {
        val rule = it.player.getOrPut(name) { PlayerRule() }
        if (!rule.species.contains(species)) rule.species.add(species)
    }

    fun removePlayerSpecies(name: String, species: String) = write {
        it.player[name]?.species?.remove(species)
    }

    fun clearPlayer(name: String) = write {
        it.player.remove(name)
    }

    fun putSprite(species: String, standard: String? = null, shiny: String? = null) = write {
        val entry = it.sprites.getOrPut(species) { SpriteEntry() }
        standard?.let { s -> entry.standard = s }
        shiny?.let { s -> entry.shiny = s }
    }

    // ── Internals ─────────────────────────────────────────────────────────────
    private fun saveLocked() {
        runCatching { file.writeText(gson.toJson(data)) }
            .onFailure { e -> logger.info("Project Ash: failed to save config: ${e.message}") }
    }

    /** Load default sprites JSON from resources into a Map<String, SpriteEntry>. */
    private fun loadSpritesFromResource(): MutableMap<String, SpriteEntry> {
        val path = "projectash/sprites.json" // src/main/resources/projectash/sprites.json
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: return mutableMapOf() // resource missing -> empty

        stream.reader(Charsets.UTF_8).use { reader ->
            val rootEl = JsonParser.parseReader(reader)
            val spritesObj: JsonObject = when {
                rootEl.isJsonObject &&
                        rootEl.asJsonObject.has("sprites") &&
                        rootEl.asJsonObject["sprites"].isJsonObject ->
                    rootEl.asJsonObject.getAsJsonObject("sprites")
                rootEl.isJsonObject -> rootEl.asJsonObject
                else -> JsonObject()
            }
            val type = object : TypeToken<Map<String, SpriteEntry>>() {}.type
            val map: Map<String, SpriteEntry> = gson.fromJson(spritesObj, type)
            return map.toMutableMap()
        }
    }
}
