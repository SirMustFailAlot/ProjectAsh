package io.github.sirmustfailalot.projectash.announcer

// ProjectAsh Classes
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.pipeline.RuleEvaluationResult

// Minecraft Classes
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

// General Logger and Other Classes
import org.slf4j.LoggerFactory
import com.google.gson.Gson
import java.time.Instant

// Discord Helper
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

object spwaningAnnouncer {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    private val gson = Gson()
    fun announceSpawn(
        pokeGlance: PokeStream.PokemonLifespan,
        announceDetails: RuleEvaluationResult
    ) {
        // Discord - Generalised Announcements for Server Wide Rules/Specials
        if (announceDetails.discordCriteria.isServerMessage && Config.data.server.discordEnabled) {
            val webhook = Config.data.server.discordWebhook
            if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                logger.info("Project Ash: Discord webhook not configured, skipping announcement")
            }
            val serverLabels = announceDetails.discordCriteria.serverLabels
            val title = (if (announceDetails.discordCriteria.serverLabels.contains("Shiny")) "✨ " else "") + "${organiseLabelsToString(serverLabels)} - ${pokeGlance.speciesWithForm}"
            val fields = if (pokeGlance.spawnSource == "Unknown") {
                listOf(
                    EmbedField("Spawn Source", "Unknown"),
                    EmbedField("Dimension", pokeGlance.spawnDimension),
                    EmbedField("Closest Player", pokeGlance.spawnClosestPlayer),
                    EmbedField("Position", "`${pokeGlance.spawnPos}`")
                )
            } else {
                listOf(
                    EmbedField("Dimension", pokeGlance.spawnDimension),
                    EmbedField("Closest Player", pokeGlance.spawnClosestPlayer),
                    EmbedField("Position", "`${pokeGlance.spawnPos}`")
                )
            }

            val rulesList = announceDetails.discordCriteria.serverRules ?: emptyList()
            val rulesString = if (rulesList.isNotEmpty()) {
                " | " + rulesList.joinToString(" | ")
            } else {
                ""
            }

            val embed = Embed(
                title = title,
                color = getEmbedColour(serverLabels),
                fields = fields,
                thumbnail = if (pokeGlance.sprite != null)
                    mapOf("url" to pokeGlance.sprite)
                else
                    null,
                footer = mapOf("text" to "ProjectAsh$rulesString"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            deliveryAnnouncer.discord(messageBody = body)
        }

        // Ingame - Player Specific Announcements
        if (announceDetails.playerCriteria.isNotEmpty()) {
            val nearMessage = if (pokeGlance.spawnDimension == "Overworld") {
                "near ${pokeGlance.spawnClosestPlayer} at ${pokeGlance.spawnPos}"
            } else {
                "near ${pokeGlance.spawnClosestPlayer} at ${pokeGlance.spawnPos} (${pokeGlance.spawnDimension})"
            }

            val messageText = when (pokeGlance.spawnSource) {
                "Unknown" -> "${pokeGlance.speciesWithForm} has somehow spawned $nearMessage"
                "Known" -> "${pokeGlance.speciesWithForm} spawned $nearMessage"
                else -> "Different Spawn"
            }

            announceDetails.playerCriteria.forEach { (playerName, notification) ->
                val ingameMessage = renderLabeledMessage(
                    notification.finalLabels.toList(),
                    messageText
                )
                deliveryAnnouncer.ingame(playerName = playerName, messageBody = ingameMessage)
            }
        }
    }

    private fun white(text: String): MutableComponent {
        val style = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(false)
        return Component.literal(text).withStyle(style)
    }

    fun renderLabeledMessage(
        labels: List<String>,
        messageTail: String,
        separator: String = " · "
    ): MutableComponent {
        val root = Component.empty()
        val labelsinOrder = organiseLabels(labels)

        if (labelsinOrder.isNotEmpty()) {
            root.append(buildColoredLabel(labelsinOrder.first()))
            for (i in 1 until labelsinOrder.size) {
                root.append(white(separator))
                root.append(buildColoredLabel(labelsinOrder[i]))
            }
            root.append(white(" "))
        }
        root.append(white(messageTail))

        return root
    }

    fun organiseLabels(labels: List<String>): List<String> {
        if (labels.isEmpty()) return emptyList()

        // Sorts with "Perfect" first, then "Shiny", then alphabetical order for anything else
        return labels.sortedWith(
            compareByDescending<String> { it.equals("Perfect", ignoreCase = true) }
                .thenByDescending { it.equals("Shiny", ignoreCase = true) }
                .thenBy { it }
        )
    }
    fun organiseLabelsToString(labels: List<String>): String {
        val prioritisedList = organiseLabels(labels)
        return prioritisedList.joinToString(" ")
    }
    private val EMBED_COLOURS = mapOf(
        "CatchEmAll" to 0xD35400,
        "Shiny" to 0xF1C40F,
        "Ultra_Beast" to 0xE74C3C,
        "Mythical" to 0x9B59B6,
        "Legendary" to 0x2ECC71,
        "Paradox" to 0x95A5A6,
        "Special" to 0xE67E22,
        "Perfect IV" to 0x3498DB
    )
    fun blendColours(colourA: Int, colourB: Int): Int {
        val r = ((colourA shr 16 and 0xFF) + (colourB shr 16 and 0xFF)) / 2
        val g = ((colourA shr 8  and 0xFF) + (colourB shr 8  and 0xFF)) / 2
        val b = ((colourA and 0xFF) + (colourB and 0xFF)) / 2
        return (r shl 16) or (g shl 8) or b
    }
    fun getEmbedColour(types: List<String>): Int {
        val priority = listOf("Perfect", "Shiny", "Ultra_Beast", "Mythical", "Legendary", "Paradox", "Special")

        val colours = priority
            .filter { it in types }
            .mapNotNull { EMBED_COLOURS[it] }

        return when {
            colours.size == 1 -> colours.first()
            colours.size >= 2 -> blendColours(colours[0], colours[1])
            else -> 0x3498DB // Default Light Blue
        }
    }
    fun buildColoredLabel(labelName: String): MutableComponent {
        val hexColor = EMBED_COLOURS[labelName] ?: 0x3498DB
        val minecraftColor = TextColor.fromRgb(hexColor)

        return Component.literal("[$labelName]")
            .withStyle { style ->
                style.withColor(minecraftColor)
                    .withBold(true) // Keeps tags punchy and easy to read
            }
    }
}