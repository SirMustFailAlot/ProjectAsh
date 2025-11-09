package io.github.sirmustfailalot

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import java.io.File

// ── Types ─────────────────────────────────────────────────────────────────────
data class SpecialRule(
    var speciesName: String = "shuckle",
    var speciesForm: String = "kantonian",
    var shinyCheck: Boolean = false
)

data class SpriteEntry(
    var standard: String = "",
    var shiny: String = ""
)

data class ServerRule(
    var ingameEnabled: Boolean = true,
    var discordEnabled: Boolean = true,
    var discordWebhook: String = "https://your.webhook.url/here",
    var discordThumbnails: Boolean = true,
    var shinyCheck: Boolean = true,
    var labelCheck: List<String> = listOf("legendary", "ultra_beast", "mythical", "paradox"),
    var specialCheck: List<SpecialRule> = emptyList()
)

data class PlayerRule(
    var enabled: Boolean = false,
    var specialCheck: MutableList<SpecialRule> = mutableListOf()
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

    // Initialise and Create
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

    fun addLabelCheck(label: String): Boolean {
        val normalised = label.trim().lowercase()
        if (normalised.isEmpty()) return false

        // Only modify if not already there
        if (data.server.labelCheck.contains(normalised)) return false

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

        if (!data.server.labelCheck.contains(normalised)) return false

        write {
            val current = it.server.labelCheck.toMutableList()
            current.remove(normalised)
            it.server.labelCheck = current
        }
        return true
    }

    fun getLabelCheck(): List<String> = data.server.labelCheck

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
