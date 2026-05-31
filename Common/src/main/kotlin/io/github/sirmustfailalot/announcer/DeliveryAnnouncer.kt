package io.github.sirmustfailalot.projectash.announcer

// ProjectAsh Classes
import io.github.sirmustfailalot.projectash.config.Config
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

object DeliveryAnnouncer {
    val logger = LoggerFactory.getLogger("ProjectAsh")
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build()
    private val io: ExecutorService = Executors.newSingleThreadExecutor {
        Thread(it, "ProjectAsh-Discord-IO").apply { isDaemon = true }
    }
    private data class CacheEntry(val url: String?, val expiresAtMs: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24h

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