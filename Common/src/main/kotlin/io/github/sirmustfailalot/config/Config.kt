package io.github.sirmustfailalot.projectash.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import java.io.File

// ── Types ─────────────────────────────────────────────────────────────────────
data class PokeRule(
    var speciesName: String = "shuckle",
    var shinyFlag: ShinyFlag = ShinyFlag.INCLUDE
)

data class CatchEmAllRule(
    var enabled: Boolean = false,
    var localSpawnsOnly: Boolean = true
)

data class SpriteEntry(
    var standard: String = "",
    var shiny: String = ""
)

enum class ShinyFlag {
    INCLUDE,
    EXCLUDE,
    ONLY
}

data class ServerRule(
    var ingameEnabled: Boolean = true,
    var discordEnabled: Boolean = true,
    var discordWebhook: String = "https://your.webhook.url/here",
    var discordThumbnails: Boolean = true,
    var checkUnknownSpawns: Boolean = false,
    var perfectCheck: Boolean = false,
    var shinyCheck: Boolean = true,
    var labelCheck: List<String> = listOf("Legendary", "Ultra Beast", "Mythical", "Paradox"),
    var specialCheck: List<PokeRule> = emptyList(),
    var blacklistCheck: List<PokeRule> = emptyList()
)

data class PlayerRule(
    var catchEmAllMode: CatchEmAllRule = CatchEmAllRule(),
    var enabled: Boolean = true,
    var specialCheck: MutableList<PokeRule> = mutableListOf()
)

data class ConfigData(
    var server: ServerRule = ServerRule(),
    var player: MutableMap<String, PlayerRule> = mutableMapOf(),
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

    // ── Helper Functions ──────────────────────────────────────────────────────
    // Server Variables
    fun setServerIngameEnabled(enabled: Boolean) = write { it.server.ingameEnabled = enabled }
    fun setServerDiscordEnabled(enabled: Boolean) = write { it.server.discordEnabled = enabled }
    fun setServerDiscordWebhook(url: String) = write { it.server.discordWebhook = url }
    fun setServerDiscordThumbnails(enabled: Boolean) = write { it.server.discordThumbnails = enabled }
    fun setServerShinyCheck(enabled: Boolean) = write { it.server.shinyCheck = enabled }
    fun setServerPerfectCheck(enabled: Boolean) = write { it.server.perfectCheck = enabled }
    fun setCheckUnknownSpawns(enabled: Boolean) = write { it.server.checkUnknownSpawns = enabled }

    fun addLabelCheck(label: String): Boolean {
        val normalised = label.trim().lowercase()
        if (normalised.isEmpty()) return false

        // Only modify if not already there
        if (data.server.labelCheck.any { it.equals(normalised, ignoreCase = true) }) return false

        write {
            val current = it.server.labelCheck.toMutableList()
            current.add(normalised)
            it.server.labelCheck = current
        }
        return true
    }

    fun removeLabelCheck(label: String): Boolean {
        val normalised = label.trim().lowercase()
        if (normalised.isEmpty()) return false

        if (data.server.labelCheck.none { it.equals(normalised, ignoreCase = true) }) return false

        write {
            val current = it.server.labelCheck.toMutableList()
            current.removeIf { it.equals(normalised, ignoreCase = true) }
            it.server.labelCheck = current
        }
        return true
    }

    fun getLabelCheck(): List<String> = data.server.labelCheck

    fun clearLabelChecks(): Boolean {
        if (data.server.labelCheck.isEmpty()) return false
        write { it.server.labelCheck = emptyList() }
        return true
    }

    fun addServerSpecialRule(species: String, shinyFlag: ShinyFlag): Boolean {
        val rule = canonicalRule(species, shinyFlag)
        // no-op if already present
        if (Config.data.server.specialCheck.any { it == rule }) return false

        write {
            val next = it.server.specialCheck.toMutableList()
            next.add(rule)
            it.server.specialCheck = next
        }
        return true
    }

    fun removeServerSpecialRule(species: String, shinyFlag: ShinyFlag): Boolean {
        val rule = canonicalRule(species, shinyFlag)
        if (!Config.data.server.specialCheck.contains(rule)) return false

        write {
            val next = it.server.specialCheck.toMutableList()
            next.remove(rule)
            it.server.specialCheck = next
        }
        return true
    }

    fun getServerSpecialRules(): List<PokeRule> = Config.data.server.specialCheck

    fun clearServerSpecialRules(): Boolean {
        if (data.server.specialCheck.isEmpty()) return false
        write { it.server.specialCheck = emptyList() }   // keep server list immutable
        return true
    }

    fun addServerBlacklistRule(species: String, shinyFlag: ShinyFlag): Boolean {
        val rule = canonicalRule(species, shinyFlag)
        // no-op if already present
        if (Config.data.server.blacklistCheck.any { it == rule }) return false

        write {
            val next = it.server.blacklistCheck.toMutableList()
            next.add(rule)
            it.server.blacklistCheck = next
        }
        return true
    }

    fun removeServerBlacklistRule(species: String, shinyFlag: ShinyFlag): Boolean {
        val rule = canonicalRule(species, shinyFlag)
        if (!Config.data.server.blacklistCheck.contains(rule)) return false

        write {
            val next = it.server.blacklistCheck.toMutableList()
            next.remove(rule)
            it.server.blacklistCheck = next
        }
        return true
    }

    fun getServerBlacklistRules(): List<PokeRule> = Config.data.server.blacklistCheck

    fun clearServerBlacklistRules(): Boolean {
        if (data.server.blacklistCheck.isEmpty()) return false
        write { it.server.blacklistCheck = emptyList() }   // keep server list immutable
        return true
    }

    fun ensurePlayer(name: String): PlayerRule {
        val trimmed = name.trim()
        var playerRule = Config.data.player[trimmed]

        if (playerRule == null) {
            // create a default entry if missing
            playerRule = PlayerRule()
            write {
                it.player[trimmed] = playerRule
            }
        }

        return playerRule
    }

    fun toggleCatchEmAllModeEnabled(playerName: String, toggle: Boolean): Boolean {
        val p = ensurePlayer(playerName)
        write {
            it.player[playerName]!!.catchEmAllMode.enabled = toggle
        }
        return true
    }

    fun toggleCatchEmAllModeLocal(playerName: String, toggle: Boolean): Boolean {
        val p = ensurePlayer(playerName)
        write {
            it.player[playerName]!!.catchEmAllMode.localSpawnsOnly = toggle
        }
        return true
    }

    fun addPlayerSpecialRule(playerName: String, species: String, shinyFlag: ShinyFlag): Boolean {
        val p = ensurePlayer(playerName)
        val rule = canonicalRule(species, shinyFlag)
        if (p.specialCheck.any { it == rule }) return false

        write {
            // ensurePlayer() already created it.data.player[playerName]
            it.player[playerName]!!.specialCheck.add(rule)
        }
        return true
    }

    fun removePlayerSpecialRule(playerName: String, species: String, shinyFlag: ShinyFlag): Boolean {
        val p = ensurePlayer(playerName)
        val rule = canonicalRule(species, shinyFlag)
        if (!p.specialCheck.contains(rule)) return false

        write {
            it.player[playerName]!!.specialCheck.remove(rule)
        }
        return true
    }

    fun getPlayerSpecialRules(playerName: String): List<PokeRule> =
        (Config.data.player[playerName] ?: PlayerRule()).specialCheck

    fun setPlayerSpecialCheck(name: String, enabled: Boolean) = write {
        val trimmed = name.trim()

        // Ensure player entry exists (like ensurePlayer, but inline here)
        val playerRule = it.player.getOrPut(trimmed) { PlayerRule() }

        // Update enabled flag
        playerRule.enabled = enabled
    }

    /** Clear ALL Special rules for a specific player. Ensures the player exists. */
    fun clearPlayerSpecialRules(name: String): Boolean {
        val key = name.trim()
        val currentSize = data.player[key]?.specialCheck?.size ?: 0
        if (currentSize == 0) return false

        write {
            val p = it.player.getOrPut(key) { PlayerRule() }
            p.specialCheck.clear()                        // player list is mutable
        }
        return true
    }

    /** Make a canonical rule for reliable equality/contains/remove operations. */
    private fun norm(s: String) = s.trim().lowercase()
    private fun canonicalRule(
        species: String,
        shinyFlag: ShinyFlag = ShinyFlag.INCLUDE
    ): PokeRule =
        PokeRule(
            speciesName = norm(species),
            shinyFlag = shinyFlag
        )

    // ── Internals ─────────────────────────────────────────────────────────────
    private fun saveLocked() {
        runCatching { file.writeText(gson.toJson(data)) }
            .onFailure { e -> logger.info("Project Ash: failed to save config: ${e.message}") }
    }
    @Synchronized
    fun write(modify: (ConfigData) -> Unit) {
        modify(data)
        saveLocked()
    }
    @Synchronized
    fun save() = saveLocked()
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

    /** Load default sprites JSON from resources into a Map<String, SpriteEntry>. */
    private fun loadSpritesFromResource(): MutableMap<String, SpriteEntry> {
        val path = "assets/projectash/sprites.json" // src/main/resources/projectash/sprites.json
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
