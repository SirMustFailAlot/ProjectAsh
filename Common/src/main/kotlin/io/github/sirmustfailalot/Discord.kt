package io.github.sirmustfailalot

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ───────────────────────────────────────────────────────────────────────────────
// PokeAPI DTOs
// ───────────────────────────────────────────────────────────────────────────────
private data class PokeApiPokemon(val sprites: Sprites?)
private data class Sprites(
    @SerializedName("front_default") val frontDefault: String?,
    @SerializedName("front_female") val frontFemale: String?,
    @SerializedName("front_shiny") val frontShiny: String?,
    @SerializedName("front_shiny_female") val frontShinyFemale: String?,
    val other: Other?
)
private data class Other(@SerializedName("official-artwork") val officialArtwork: OfficialArtwork?)
private data class OfficialArtwork(
    @SerializedName("front_default") val frontDefault: String?,
    @SerializedName("front_shiny") val frontShiny: String?
)

// For species → default variety fallback
private data class SpeciesDTO(val varieties: List<Variety>?)
private data class Variety(
    @SerializedName("is_default") val isDefault: Boolean,
    val pokemon: NamedUrl?
)
private data class NamedUrl(val name: String?, val url: String?)

// ───────────────────────────────────────────────────────────────────────────────
// Discord payload DTOs
// ───────────────────────────────────────────────────────────────────────────────
private data class EmbedField(val name: String, val value: String, val inline: Boolean = true)
private data class Embed(
    val title: String,
    val description: String? = null,
    val color: Int? = null,
    val fields: List<EmbedField> = emptyList(),
    val thumbnail: Map<String, String>? = null,
    val footer: Map<String, String>? = null,
    val timestamp: String? = null
)
private data class WebhookPayload(
    val content: String? = null,
    val allowed_mentions: Map<String, List<String>> = mapOf("parse" to emptyList()),
    val embeds: List<Embed>
)

object Discord {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    private val gson = Gson()

    // Single IO worker: keeps HTTP off the tick thread, reduces rate-limit headaches
    private val io: ExecutorService = Executors.newSingleThreadExecutor {
        Thread(it, "ProjectAsh-Discord-IO").apply { isDaemon = true }
    }

    // TTL cache (24h) for sprite URLs
    private data class CacheEntry(val url: String?, val expiresAtMs: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24h

    // ────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────
    fun send(
        dimension: String,
        playerName: String?,
        spawnType: String,
        shiny: Boolean,
        species: String,
        speciesPlusForm: String,
        posValue: String
    ) {
        // Return immediately; do everything off-thread
        io.execute {
            try {
                // Adjust this import to match where your Config lives:
                // e.g. import com.projectash.config.Config
                val webhook = Config.get("discord_webhook").toString().trim()
                if (webhook.isNullOrBlank()) {
                    logger.info("Project Ash: discord_webhook missing in config; skipping webhook send.")
                    return@execute
                }

                // Sprite lookup (blocking here is OK—we're on the IO thread)
                val spriteUrl = frontSpriteCached(species, shiny)

                val title = (if (shiny) "✨ " else "") + "$spawnType — $speciesPlusForm"
                val fields = listOf(
                    EmbedField("Species", speciesPlusForm),
                    EmbedField("Type", spawnType),
                    EmbedField("Dimension", dimension),
                    EmbedField("Position", "`$posValue`"),
                    EmbedField("Closest Player", playerName ?: "Unknown")
                )

                val embed = Embed(
                    title = title,
                    description = "A new Pokémon has appeared!",
                    color = if (shiny) 0xE91E63 else 0xF1C40F,
                    fields = fields,
                    thumbnail = spriteUrl?.let { mapOf("url" to it) },
                    footer = mapOf("text" to "ProjectAsh"),
                    timestamp = Instant.now().toString()
                )

                val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))

                Fuel.post(webhook)
                    .jsonBody(body)
                    .response { _, response, result ->
                        when (result) {
                            is Result.Success -> {
                                // no-op; you only want error logs
                            }
                            is Result.Failure -> {
                                val code = response.statusCode
                                val data = runCatching { String(response.data) }.getOrNull()

                                // Handle rate limit once
                                if (code == 429 && data != null) {
                                    val retryAfterMs = parseRetryAfterMs(data)
                                    if (retryAfterMs != null) {
                                        io.execute {
                                            try {
                                                Thread.sleep(retryAfterMs)
                                                Fuel.post(webhook).jsonBody(body).response { _, r2, res2 ->
                                                    if (res2 is Result.Failure) {
                                                        logger.info(
                                                            "Project Ash: Discord retry failed ${r2.statusCode}: ${res2.getException().message}"
                                                        )
                                                    }
                                                }
                                            } catch (t: Throwable) {
                                                logger.info("Project Ash: Discord retry error: ${t.message}")
                                            }
                                        }
                                        return@response
                                    }
                                }

                                logger.info("Project Ash: Discord send failed $code: ${result.getException().message} ${data ?: ""}")
                            }
                        }
                    }
            } catch (t: Throwable) {
                logger.info("Project Ash: Discord send() error: ${t.message}")
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Sprite lookup (cached, blocking ON THE IO THREAD ONLY)
    // ────────────────────────────────────────────────────────────────────────
    private fun frontSpriteCached(speciesName: String, shiny: Boolean): String? {
        val key = "${normalize(speciesName)}|${if (shiny) "shiny" else "default"}"
        val now = System.currentTimeMillis()

        // Fresh cache hit
        cache[key]?.let { if (it.expiresAtMs > now) return it.url }

        // Fetch inline (we're on the IO thread)
        val url = fetchFrontSprite(speciesName, shiny)
            ?: fetchFrontSpriteBySpeciesVariety(speciesName, shiny) // 404 fallback

        cache[key] = CacheEntry(url, now + CACHE_TTL_MS)
        return url
    }

    /** Try /pokemon/{name} first (normalized). */
    private fun fetchFrontSprite(speciesName: String, shiny: Boolean): String? {
        val name = normalize(speciesName)
        val apiUrl = "https://pokeapi.co/api/v2/pokemon/$name"

        val (_, response, result) = Fuel.get(apiUrl)
            .header("User-Agent", "ProjectAsh/1.0")
            .timeout(8_000)
            .timeoutRead(8_000)
            .responseString()

        return when (result) {
            is Result.Success -> runCatching {
                val p = gson.fromJson(result.get(), PokeApiPokemon::class.java)
                val s = p.sprites ?: return null
                if (shiny) {
                    s.other?.officialArtwork?.frontShiny ?: s.frontShiny ?: s.frontShinyFemale
                } else {
                    s.other?.officialArtwork?.frontDefault ?: s.frontDefault ?: s.frontFemale
                }
            }.getOrElse {
                logger.info("Project Ash: sprite JSON parse error for '$name' (from '$speciesName'): ${it.message}")
                null
            }
            is Result.Failure -> {
                if (response.statusCode != 404) {
                    logger.info("Project Ash: PokeAPI ${response.statusCode} for '$name' (from '$speciesName'): ${result.getException().message}")
                }
                null
            }
        }
    }

    /** If /pokemon/{name} 404s, try species → default variety → /pokemon/{variety}. */
    private fun fetchFrontSpriteBySpeciesVariety(speciesName: String, shiny: Boolean): String? {
        val species = normalize(speciesName)
        val speciesUrl = "https://pokeapi.co/api/v2/pokemon-species/$species"

        val (_, response, result) = Fuel.get(speciesUrl)
            .header("User-Agent", "ProjectAsh/1.0")
            .timeout(8_000)
            .timeoutRead(8_000)
            .responseString()

        if (result is Result.Failure) {
            logger.info("Project Ash: species lookup ${response.statusCode} for '$species' (from '$speciesName'): ${result.getException().message}")
            return null
        }

        val defaultVarietyName = runCatching {
            val dto = gson.fromJson((result as Result.Success).get(), SpeciesDTO::class.java)
            val v = dto.varieties?.firstOrNull { it.isDefault } ?: dto.varieties?.firstOrNull()
            v?.pokemon?.name
        }.getOrNull()

        return if (defaultVarietyName != null) {
            fetchFrontSprite(defaultVarietyName, shiny)
        } else {
            logger.info("Project Ash: no varieties for species '$species' (from '$speciesName')")
            null
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────
    private fun parseRetryAfterMs(body: String): Long? = runCatching {
        @Suppress("UNCHECKED_CAST")
        val obj = gson.fromJson(body, Map::class.java) as Map<*, *>
        val v = obj["retry_after"] ?: return null
        val seconds = when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: return null
            else -> return null
        }
        (seconds * 1000).toLong()
    }.getOrNull()

    /** Strong name normalization for PokeAPI resource keys. */
    private fun normalize(name: String): String =
        name.trim().lowercase()
            .replace(' ', '-')    // "Mr Mime" -> "mr-mime"
            .replace(":", "-")    // "Type: Null" -> "type-null"
            .replace(".", "")     // "Mr. Mime" -> "mr-mime"
            .replace("'", "")     // "Farfetch'd" -> "farfetchd"
            .replace("é", "e")    // "Flabébé" -> "flabebe"
            .replace("♀", "-f")   // "Nidoran♀" -> "nidoran-f"
            .replace("♂", "-m")   // "Nidoran♂" -> "nidoran-m"
}
