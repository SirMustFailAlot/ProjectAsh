package io.github.sirmustfailalot

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

object Config {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File("config/ProjectAsh.json")
    private var config: JsonObject = JsonObject()

    init {
        if (!configFile.exists()) {
            createDefaultConfig()
        } else {
            loadConfig()
        }
    }

    // ---- Defaults ----
    private fun defaults(): JsonObject = JsonObject().apply {
        addProperty("discord_announcements", true)
        addProperty("discord_webhook", "https://your.webhook.url/here")
        addProperty("server_announcements", true)
    }

    // ---- Create / Load / Save ----
    private fun createDefaultConfig() {
        configFile.parentFile.mkdirs()
        config = defaults()
        saveConfig()
    }

    private fun loadConfig() {
        val content = runCatching { configFile.readText() }.getOrDefault("")
        config = runCatching { JsonParser.parseString(content).asJsonObject }
            .getOrElse { defaults() }

        // Backfill any missing keys from defaults
        val def = defaults()
        for ((k, v) in def.entrySet()) {
            if (!config.has(k)) config.add(k, v)
        }
        // Persist backfilled defaults
        saveConfig()
    }

    private fun saveConfig() {
        configFile.writeText(gson.toJson(config))
    }

    // ---- Public API (same shape as before) ----
    fun get(key: String): Any? {
        val el = config.get(key) ?: return null
        return jsonElementToKotlin(el)
    }

    fun set(key: String, value: Any) {
        when (value) {
            is String   -> config.addProperty(key, value)
            is Number   -> config.addProperty(key, value)
            is Boolean  -> config.addProperty(key, value)
            is JsonElement -> config.add(key, value)
            is List<*>  -> config.add(key, gson.toJsonTree(value))
            is Map<*,*> -> config.add(key, gson.toJsonTree(value))
            else -> throw IllegalArgumentException("Unsupported value type: ${value::class}")
        }
        saveConfig()
    }

    // ---- Helpers ----
    private fun jsonElementToKotlin(el: JsonElement): Any? {
        if (el.isJsonNull) return null
        if (el.isJsonPrimitive) {
            val p = el.asJsonPrimitive
            return when {
                p.isBoolean -> p.asBoolean
                p.isNumber  -> {
                    val s = p.asString
                    s.toLongOrNull() ?: s.toDoubleOrNull() ?: s
                }
                else        -> p.asString
            }
        }
        if (el.isJsonArray)  return gson.fromJson(el, List::class.java)
        if (el.isJsonObject) return gson.fromJson(el, Map::class.java)
        return null
    }
}
