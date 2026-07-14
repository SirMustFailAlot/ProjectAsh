package io.github.sirmustfailalot.projectash.announcer

// ProjectAsh Classes
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.pipeline.RuleEvaluationResult
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers.server

// Minecraft Classes
import net.minecraft.network.chat.MutableComponent

// General Logger and Other Classes
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import com.google.gson.Gson

object DeliveryAnnouncer {
    val logger = LoggerFactory.getLogger("ProjectAsh")
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

    fun executeBroadcast(
        iconPrefix: String = "",
        eventState: String,
        pokeGlance: PokeStream.PokemonLifespan,
        announceDetails: RuleEvaluationResult,
        customDiscordFields: List<EmbedField> = emptyList(),
        customThumbnail: String? = null,
        inGameText: String
    ) {
        val serverLabels = announceDetails.discordCriteria.serverLabels
        val cleanLabelStr = UtilityAnnouncer.organiseLabelsToString(serverLabels)

        if (announceDetails.discordCriteria.isServerMessage && Config.data.server.discordEnabled) {
            val webhook = Config.data.server.discordWebhook
            if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                logger.info("Project Ash: Discord webhook not configured, skipping announcement")
                return
            }

            val title = "$iconPrefix$eventState - $cleanLabelStr - ${pokeGlance.speciesWithForm}"

            // Build fields list sequentially; injects Poke Tags automatically for consistency
            val fields = mutableListOf<EmbedField>()
            if (pokeGlance.spawnSource == "Unknown") {
                fields.add(EmbedField("Spawn Source", "Unknown"))
            }
            fields.add(EmbedField("Poke Traits", cleanLabelStr.ifBlank { "None" }))
            fields.addAll(customDiscordFields)

            val rulesList = announceDetails.discordCriteria.serverRules
            val rulesString = if (rulesList.isNotEmpty()) " | " + rulesList.joinToString(" | ") else ""

            val embed = Embed(
                title = title,
                color = UtilityAnnouncer.getEmbedColour(serverLabels),
                fields = fields,
                thumbnail = customThumbnail?.let { mapOf("url" to it) },
                footer = mapOf("text" to "ProjectAsh$rulesString"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            discord(messageBody = body)
        }

        if (announceDetails.playerCriteria.isNotEmpty()) {
            announceDetails.playerCriteria.forEach { (playerName, notification) ->
                val ingameMessage = UtilityAnnouncer.renderLabeledMessage(
                    notification.finalLabels.toList(),
                    inGameText,
                    pokeGlance
                )
                ingame(playerName = playerName, messageBody = ingameMessage)
            }
        }
    }

    fun discord(messageBody: String) {
        val webhook = Config.data.server.discordWebhook
        sendDiscordMessage(webhook, messageBody)
    }

    fun ingame(playerName: String, messageBody: MutableComponent) {
        val player = server!!.playerList.getPlayerByName(playerName)?: return
        player.sendSystemMessage(messageBody)
    }

    fun sendDiscordMessage(webhook: String, body: String) {
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
                logger.info("Project Ash: Discord send error: ${it.message}, webhook: $webhook, body: $body")
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
}