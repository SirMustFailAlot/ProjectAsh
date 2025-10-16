package io.github.sirmustfailalot

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
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
import kotlin.compareTo

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
private data class EmbedField(val name: String, val value: String, val inline: Boolean = false)
private data class Embed(
    val author: Map<String, String>? = null,
    val title: String? = null,
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
    fun spawn(
        server: MinecraftServer?,           // Required for config alert
        dimension: String,
        playerName: String?,
        spawnType: List<String>,
        shiny: Boolean,
        species: String,
        speciesPlusForm: String,
        posValue: String
    ) {
        io.execute {
            try {
                val webhookEnabled = Config.data.discord.enabled
                val Thumbnails = Config.data.discord.thumbnails
                if (webhookEnabled) {
                    val webhook = Config.data.discord.webhook
                    if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                        Announcement.discordWebhookFail(server)
                        return@execute
                    }

                    val spawnTypeString = labelsToSpawnTypeString(spawnType)
                    val normalisedSpecies = normalize(species)
                    val spriteUrl = if (shiny) {
                        Config.data.sprites[normalisedSpecies]?.shiny
                    } else {
                        Config.data.sprites[normalisedSpecies]?.standard}
                    val title = (if (shiny) "✨ " else "") + "$spawnTypeString — $speciesPlusForm"
                    val fields = listOf(
                        EmbedField("Dimension", dimension),
                        EmbedField("Closest Player", playerName ?: "Unknown"),
                        EmbedField("Position", "`$posValue`")
                    )

                    val embed = Embed(
                        title = title,
                        color = if (shiny) 0xE91E63 else 0xF1C40F,
                        fields = fields,
                        thumbnail = if (Thumbnails && spriteUrl != null)
                            mapOf("url" to spriteUrl)
                        else
                            null,
                        footer = mapOf("text" to "ProjectAsh"),
                        timestamp = Instant.now().toString()
                    )

                    val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
                    sendMessage(webhook, body)
                }
            } catch (t: Throwable) {
                logger.info("Project Ash: Discord send() error: ${t.message}")
            }
        }
    }

    fun captureOrFainted(
        eventType: String,
        server: MinecraftServer?,           // Required for config alert
        playerName: String? = null,
        spawnType: List<String>,
        species: String,
        speciesPlusForm: String
    ) {
        io.execute {
            try {
                val webhookEnabled = Config.data.discord.enabled
                val Thumbnails = Config.data.discord.thumbnails
                if (webhookEnabled) {
                    val webhook = Config.data.discord.webhook
                    if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                        Announcement.discordWebhookFail(server)
                        return@execute
                    }

                    val shiny = when {spawnType.any { it.equals("shiny", ignoreCase = true)} -> true else -> false}

                    val spawnTypeString = labelsToSpawnTypeString(spawnType)
                    val normalisedSpecies = normalize(species)
                    val spriteUrl = if (shiny) {
                        Config.data.sprites[normalisedSpecies]?.shiny
                    } else {
                        Config.data.sprites[normalisedSpecies]?.standard}
                    val title = if (eventType == "Captured") {"✅ $eventType $speciesPlusForm!"} else {"❌ $speciesPlusForm $eventType!"}
                    val fields = if (eventType == "Captured")
                        {
                            listOf(
                                EmbedField("Spawn Type", spawnTypeString),
                                EmbedField("Player", playerName ?: "Unknown"))
                        } else {
                            listOf(
                                EmbedField("Spawn Type", spawnTypeString))}

//                    val iconUrl = when {
//                        spawnType.any { it.equals("legendary", ignoreCase = true) } ->
//                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/master-ball.png" // URL A
//                        spawnType.any { it.equals("ultra-beast", ignoreCase = true) } ->
//                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/ultra-ball.png"  // URL B
//                        spawnType.any { it.equals("shiny", ignoreCase = true) } ->
//                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/luxury-ball.png" // URL C
//                        else ->
//                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png"   // URL D
//                    }

                    val embed = Embed(
                        title = title,
                        color = if (shiny) 0xE91E63 else 0xF1C40F,
                        fields = fields,
                        thumbnail = if (eventType == "Captured" && Thumbnails && spriteUrl != null)
                        {mapOf("url" to spriteUrl)}
                        else {
                            if (eventType == "Fainted")
                            {mapOf("url" to "https://s-media-cache-ak0.pinimg.com/600x315/b1/20/08/b120087f3a904bda147251beaedf5755.jpg")} else {null}
                        },
                        footer = mapOf("text" to "ProjectAsh"),
                        timestamp = Instant.now().toString()
                    )

                    val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
                    sendMessage(webhook, body)
                }
            } catch (t: Throwable) {
                logger.info("Project Ash: Discord send() error: ${t.message}")
            }
        }
    }

    fun sendMessage(webhook: String, body: String) {
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

    fun labelsToSpawnTypeString(labels: List<String>): String {
        if (labels.isEmpty()) return ""

        // Normalize and unique them
        val normalized = labels
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

        // Ensure "Shiny" is first if it exists (case-insensitive)
        val shinyFirst = normalized.sortedWith(compareByDescending<String> {
            it.equals("shiny", ignoreCase = true)
        })

        // Combine with spaces
        return shinyFirst.joinToString(" ")
    }

}
