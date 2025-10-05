package io.github.sirmustfailalot

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object Config {
    private val configFile = File("config/ProjectAsh.json")
    private var config = JSONObject()

    init {
        if (!configFile.exists()) {
            createDefaultConfig()
        } else {
            loadConfig()
        }
    }

    private fun createDefaultConfig() {
        configFile.parentFile.mkdirs()
        config.put("discord_announcements", true)
        config.put("discord_webhook", "https://your.webhook.url/here")
        config.put("server_announcements", true)
        saveConfig()
    }

    private fun loadConfig() {
        val content = configFile.readText()
        config = JSONObject(content)
    }

    private fun saveConfig() {
        configFile.writeText(config.toString(4))
    }

    fun get(key: String): Any? {
        return config.opt(key)
    }

    fun set(key: String, value: Any) {
        when (value) {
            is String, is Number, is Boolean -> config.put(key, value)
            is List<*> -> config.put(key, JSONArray(value))
            is Map<*, *> -> config.put(key, JSONObject(value))
            is JSONObject, is JSONArray -> config.put(key, value)
            else -> throw IllegalArgumentException("Unsupported value type: ${value::class}")
        }
        saveConfig()
    }
}
