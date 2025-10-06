package io.github.sirmustfailalot

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build()

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

                val request = HttpRequest.newBuilder(URI.create(webhook))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()

                http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept { resp ->
                        val code = resp.statusCode()
                        if (code == 429) {
                            val retry = resp.headers().firstValue("Retry-After").orElse("0").toDoubleOrNull()
                            if (retry != null && retry > 0.0) {
                                io.execute {
                                    try {
                                        Thread.sleep((retry * 1000).toLong())
                                        http.send(request, HttpResponse.BodyHandlers.ofString()).also { r2 ->
                                            if (r2.statusCode() >= 300) {
                                                logger.info("Project Ash: Discord retry failed ${r2.statusCode()}: ${r2.body()}")
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        logger.info("Project Ash: Discord retry error: ${t.message}")
                                    }
                                }
                            }
                        } else if (code >= 300) {
                            logger.info("Project Ash: Discord send failed $code: ${resp.body()}")
                        }
                    }
                    .exceptionally {
                        logger.info("Project Ash: Discord send error: ${it.message}")
                        null
                    }
            } catch (t: Throwable) {
                logger.info("Project Ash: Discord send() error: ${t.message}")
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // HTTP helper for PokeAPI GETs
    // ────────────────────────────────────────────────────────────────────────
    private data class HttpText(val code: Int, val body: String?)
    private fun httpGet(url: String): HttpText = try {
        val req = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "ProjectAsh/1.0")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build()
        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        HttpText(resp.statusCode(), resp.body())
    } catch (t: Throwable) {
        logger.info("Project Ash: HTTP GET error for '$url': ${t.message}")
        HttpText(599, null) // sentinel for network error
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

        val (code, body) = httpGet(apiUrl)

        return if (code == 200 && body != null) {
            runCatching {
                val p = gson.fromJson(body, PokeApiPokemon::class.java)
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
        } else {
            if (code != 404) {
                logger.info("Project Ash: PokeAPI $code for '$name' (from '$speciesName')")
            }
            null
        }
    }

    /** If /pokemon/{name} 404s, try species → default variety → /pokemon/{variety}. */
    private fun fetchFrontSpriteBySpeciesVariety(speciesName: String, shiny: Boolean): String? {
        val species = normalize(speciesName)
        val speciesUrl = "https://pokeapi.co/api/v2/pokemon-species/$species"

        val (code, body) = httpGet(speciesUrl)
        if (code != 200 || body == null) {
            logger.info("Project Ash: species lookup $code for '$species' (from '$speciesName')")
            return null
        }

        val defaultVarietyName = runCatching {
            val dto = gson.fromJson(body, SpeciesDTO::class.java)
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
