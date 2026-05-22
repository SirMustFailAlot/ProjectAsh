package io.github.sirmustfailalot

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

data class DiscordPokemonStatus(val name: String, val level: Int, val isShiny: Boolean, val isFainted: Boolean)
data class DiscordParticipantSummary(val displayName: String, val isWinner: Boolean, val pokemonList: List<DiscordPokemonStatus>)

private data class SpeciesDTO(val varieties: List<Variety>?)
private data class Variety(
    @SerializedName("is_default") val isDefault: Boolean,
    val pokemon: NamedUrl?
)
private data class NamedUrl(val name: String?, val url: String?)

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
    private val io: ExecutorService = Executors.newSingleThreadExecutor {
        Thread(it, "ProjectAsh-Discord-IO").apply { isDaemon = true }
    }
    private data class CacheEntry(val url: String?, val expiresAtMs: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24h

    fun spawn(
        server: MinecraftServer?,
        announceSource: String,
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
                val webhookEnabled = Config.data.server.discordEnabled
                val Thumbnails = Config.data.server.discordThumbnails
                if (webhookEnabled) {
                    val webhook = Config.data.server.discordWebhook
                    if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                        Announcement.discordWebhookFail(server)
                        return@execute
                    }

                    val spawnTypeString = labelsToSpawnTypeString(spawnType)
                    val normalisedSpecies = normalise(species)
                    val spriteUrl = if (shiny) {
                        Config.data.sprites[normalisedSpecies]?.shiny
                    } else {
                        Config.data.sprites[normalisedSpecies]?.standard}
                    val title = (if (shiny) "✨ " else "") + "$spawnTypeString — $speciesPlusForm"

                    val fields = if (announceSource === "Unknown") {
                        listOf(
                            EmbedField("Spawn Source", "Unknown"),
                            EmbedField("Dimension", dimension),
                            EmbedField("Closest Player", playerName ?: "Unknown"),
                            EmbedField("Position", "`$posValue`")
                        )
                    } else {
                        listOf(
                            EmbedField("Dimension", dimension),
                            EmbedField("Closest Player", playerName ?: "Unknown"),
                            EmbedField("Position", "`$posValue`")
                        )
                    }

                    val embed = Embed(
                        title = title,
                        color = getEmbedColour(spawnType),
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

    fun announcement(
        eventType: String,
        server: MinecraftServer?,
        playerName: String? = null,
        spawnType: List<String>,
        species: String,
        speciesPlusForm: String
    ) {
        io.execute {
            try {
                val webhookEnabled = Config.data.server.discordEnabled
                val Thumbnails = Config.data.server.discordThumbnails
                if (webhookEnabled) {
                    val webhook = Config.data.server.discordWebhook
                    if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                        Announcement.discordWebhookFail(server)
                        return@execute
                    }

                    val shiny = when {spawnType.any { it.equals("shiny", ignoreCase = true)} -> true else -> false}

                    val spawnTypeString = labelsToSpawnTypeString(spawnType)
                    val normalisedSpecies = normalise(species)
                    val spriteUrl = if (shiny) {
                        Config.data.sprites[normalisedSpecies]?.shiny
                    } else {
                        Config.data.sprites[normalisedSpecies]?.standard}
                    val title = if (eventType == "Captured") {
                        "✅ $eventType $speciesPlusForm!"
                    } else if (eventType == "Hatched") {
                        "🐣 $speciesPlusForm $eventType!"
                    } else {
                        "❌ $speciesPlusForm $eventType!"
                    }
                    val fields = if (eventType == "Captured")
                    {
                        listOf(
                            EmbedField("Spawn Type", spawnTypeString),
                            EmbedField("Player", playerName ?: "Unknown"))
                    } else if (eventType == "Hatched")
                    {
                        listOf(
                            EmbedField("Hatch Type", spawnTypeString),
                            EmbedField("Player", playerName ?: "Unknown"))
                    }else {
                        listOf(
                            EmbedField("Spawn Type", spawnTypeString))}

                    val embed = Embed(
                        title = title,
                        color = getEmbedColour(spawnType),
                        fields = fields,
                        thumbnail = if ((eventType == "Captured" || eventType == "Hatched") && Thumbnails && spriteUrl != null)
                        {mapOf("url" to spriteUrl)}
                        else {
                            if (eventType == "Fainted")
                            {mapOf("url" to "https://s-media-cache-ak0.pinimg.com/600x315/b1/20/08/b120087f3a904bda147251beaedf5755.jpg")}
                            else if (eventType == "Despawned")
                            {mapOf("url" to "https://i.pinimg.com/originals/a9/48/e0/a948e0a1af81e162fe766faeeba3bc51.jpg")}
                            else {null}
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

    fun battleFinished(
        server: MinecraftServer?,
        summaries: List<DiscordParticipantSummary>
    ) {
        io.execute {
            try {
                if (!Config.data.server.discordEnabled) return@execute
                val webhook = Config.data.server.discordWebhook
                if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                    Announcement.discordWebhookFail(server)
                    return@execute
                }

                val fields = summaries.map { summary ->
                    val outcomeTag = if (summary.isWinner) "🏆 WINNER" else "💀 DEFEAT"

                    val partyLines = summary.pokemonList.joinToString("\n") { poke ->
                        // FIX: Detect placeholder fog-of-war string to suppress Level strings and render a neutral grey/white circle
                        if (poke.name == "???") {
                            "🟪 *${poke.name}*"
                        } else {
                            val statusEmoji = if (poke.isFainted) "🟥" else "🟩"
                            val shinySparkle = if (poke.isShiny) "✨ " else ""
                            "$statusEmoji $shinySparkle${poke.name} *(Lv. ${poke.level})*"
                        }
                    }.ifBlank { "*No Pokémon brought*" }

                    EmbedField(
                        name = "${summary.displayName}\n($outcomeTag)",
                        value = partyLines,
                        inline = true
                    )
                }

                val teamWonAll = summaries.any { it.isWinner }
                val cardColor = if (teamWonAll) 0x2ECC71 else 0xE74C3C

                val embed = Embed(
                    title = "⚔️ Trainer Battle Encounter Finished",
                    color = cardColor,
                    fields = fields,
                    footer = mapOf("text" to "ProjectAsh · Battle Tracker"),
                    timestamp = Instant.now().toString()
                )

                val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
                sendMessage(webhook, body)
            } catch (t: Throwable) {
                logger.info("Project Ash: Discord battleFinished() error: ${t.message}")
            }
        }
    }

    private val EMBED_COLOURS = mapOf(
        "shiny" to 0xF1C40F,
        "ultra_beast" to 0xE74C3C,
        "mythical" to 0x9B59B6,
        "legendary" to 0x2ECC71,
        "paradox" to 0x95A5A6,
        "special" to 0xE67E22,
        "perfect_iv" to 0x3498DB
    )

    fun blendColours(colourA: Int, colourB: Int): Int {
        val r = ((colourA shr 16 and 0xFF) + (colourB shr 16 and 0xFF)) / 2
        val g = ((colourA shr 8  and 0xFF) + (colourB shr 8  and 0xFF)) / 2
        val b = ((colourA and 0xFF) + (colourB and 0xFF)) / 2
        return (r shl 16) or (g shl 8) or b
    }

    fun getEmbedColour(types: List<String>): Int {
        val normalised = types.map { it.lowercase() }

        val priority = listOf("perfect", "shiny", "ultra_beast", "mythical", "legendary", "paradox", "special")

        // Collect colours in priority order
        val colours = priority
            .filter { it in normalised }
            .mapNotNull { EMBED_COLOURS[it] }

        return when {
            colours.isEmpty() -> 0x3498DB // fallback: bright cobblemon-blue
            colours.size == 1 -> colours.first()
            colours.size >= 2 -> blendColours(colours[0], colours[1])
            else -> 0x3498DB
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

    private fun normalise(name: String): String =
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

        val LABELS = mapOf(
            "perfect"     to "Perfect",
            "shiny"       to "Shiny",
            "legendary"   to "Legendary",
            "mythical"    to "Mythical",
            "ultra_beast" to "Ultra Beast",
            "paradox"     to "Paradox",
            "special"     to "Special",
            "projectash"  to "Project Ash"
        )

        val normalised = labels
            .mapNotNull { raw ->
                val key = raw.trim().lowercase()
                if (key.isEmpty()) return@mapNotNull null
                LABELS[key] ?: key.replaceFirstChar { it.uppercase() }
            }
            .distinct()

        val prioritised = normalised.sortedWith(
            compareByDescending<String> { it.equals("Perfect", ignoreCase = true) }
                .thenByDescending { it.equals("Shiny", ignoreCase = true) }
        )

        return prioritised.joinToString(" ")
    }
}