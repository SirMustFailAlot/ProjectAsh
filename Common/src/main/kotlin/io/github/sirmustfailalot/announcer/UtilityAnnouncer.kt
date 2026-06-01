package io.github.sirmustfailalot.projectash.announcer

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor

data class EmbedField(val name: String, val value: String, val inline: Boolean = false)

data class Embed(
    val author: Map<String, String>? = null,
    val title: String? = null,
    val color: Int? = null,
    val fields: List<EmbedField> = emptyList(),
    val thumbnail: Map<String, String>? = null,
    val footer: Map<String, String>? = null,
    val timestamp: String? = null
)

data class WebhookPayload(
    val content: String? = null,
    val allowed_mentions: Map<String, List<String>> = mapOf("parse" to emptyList()),
    val embeds: List<Embed>
)

object UtilityAnnouncer {
    val EMBED_COLOURS = mapOf(
        "CatchEmAll" to 0xD35400,
        "Shiny" to 0xF1C40F,
        "Ultra Beast" to 0xE74C3C,
        "Mythical" to 0x9B59B6,
        "Legendary" to 0x2ECC71,
        "Paradox" to 0x95A5A6,
        "Special" to 0xE67E22,
        "Perfect IV" to 0x3498DB
    )

    fun renderLabeledMessage(labels: List<String>, messageTail: String, separator: String = " · "): MutableComponent {
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
        return labels.sortedWith(
            compareByDescending<String> { it.equals("Perfect", ignoreCase = true) }
                .thenByDescending { it.equals("Shiny", ignoreCase = true) }
                .thenBy { it }
        )
    }

    fun organiseLabelsToString(labels: List<String>): String {
        return organiseLabels(labels).joinToString(" ")
    }

    fun buildColoredLabel(labelName: String): MutableComponent {
        val hexColor = EMBED_COLOURS[labelName] ?: 0x3498DB
        val minecraftColor = TextColor.fromRgb(hexColor)

        return Component.literal("[$labelName]")
            .withStyle { style ->
                style.withColor(minecraftColor).withBold(true)
            }
    }

    fun getEmbedColour(types: List<String>): Int {
        val priority = listOf("Perfect", "Shiny", "Ultra Beast", "Mythical", "Legendary", "Paradox", "Special")
        val colours = priority.filter { it in types }.mapNotNull { EMBED_COLOURS[it] }

        return when {
            colours.size == 1 -> colours.first()
            colours.size >= 2 -> blendColours(colours[0], colours[1])
            else -> 0x3498DB
        }
    }

    private fun blendColours(colourA: Int, colourB: Int): Int {
        val r = ((colourA shr 16 and 0xFF) + (colourB shr 16 and 0xFF)) / 2
        val g = ((colourA shr 8  and 0xFF) + (colourB shr 8  and 0xFF)) / 2
        val b = ((colourA and 0xFF) + (colourB and 0xFF)) / 2
        return (r shl 16) or (g shl 8) or b
    }

    private fun white(text: String): MutableComponent {
        val style = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(false)
        return Component.literal(text).withStyle(style)
    }
}