package io.github.sirmustfailalot.projectash.announcer

import io.github.sirmustfailalot.projectash.pipeline.BattleStream
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
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

    fun renderLabeledMessage(
        labels: List<String>,
        messageTail: String,
        pokeGlance: PokeStream.PokemonLifespan,
        separator: String = " · "
    ): MutableComponent {
        val root = Component.empty()
        val labelsinOrder = organiseLabels(labels)

        // 1. Render the prefix labels
        if (labelsinOrder.isNotEmpty()) {
            root.append(buildColoredLabel(labelsinOrder.first()))
            for (i in 1 until labelsinOrder.size) {
                root.append(white(separator))
                root.append(buildColoredLabel(labelsinOrder[i]))
            }
            root.append(white(" "))
        }

        // 2. Identify the coordinate block at the end of messageTail
        val spawnPos = pokeGlance.spawnPos
        val dimension = pokeGlance.spawnDimension

        var mainText = messageTail
        var coordSuffixComponent: MutableComponent? = null

        if (spawnPos != null) {
            // Define the two possible suffixes we want to intercept at the end of the message
            val suffixWithDim = "$spawnPos ($dimension)"
            val suffixWithoutDim = spawnPos

            // Check which suffix is present at the tail of the message
            val matchedSuffix = when {
                mainText.endsWith(suffixWithDim) -> suffixWithDim
                mainText.endsWith(suffixWithoutDim) -> suffixWithoutDim
                else -> null
            }

            if (matchedSuffix != null) {
                // Split the text: keep everything before the coordinates as the mainText
                mainText = mainText.substring(0, mainText.length - matchedSuffix.length)

                // Map plain text dimension names to valid Minecraft namespace keys
                val mcDimension = when (dimension?.lowercase()) {
                    "overworld" -> "minecraft:overworld"
                    "nether" -> "minecraft:the_nether"
                    "end", "the_end" -> "minecraft:the_end"
                    else -> dimension
                }

                val commandSpawnPos = spawnPos.replace(",","")
                val tpCommand = if (mcDimension != null) {
                    "/execute in $mcDimension run tp @s $commandSpawnPos"
                } else {
                    "/tp @s $commandSpawnPos"
                }

                // Build the clickable component using the matched suffix text
                coordSuffixComponent = Component.literal(matchedSuffix)
                    .withStyle { style ->
                        style
                            .withColor(0xFFAA00) // Gold
                            .withUnderlined(true)
                            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                            .withHoverEvent(HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to teleport to these coordinates!")
                            ))
                    }
            }
        }

        // 3. Render the main text body (with hoverable Pokemon name if applicable)
        val pokemonShowcase = true
        if (pokemonShowcase && pokeGlance.speciesWithForm != null && pokeGlance.pokemonItemStack != null && mainText.contains(pokeGlance.speciesWithForm)) {
            val index = mainText.indexOf(pokeGlance.speciesWithForm)
            val before = mainText.substring(0, index)
            val name = mainText.substring(index, index + pokeGlance.speciesWithForm.length)
            val after = mainText.substring(index + pokeGlance.speciesWithForm.length)

            if (before.isNotEmpty()) root.append(white(before))

            val hoverableName = white(name).withStyle { style ->
                style.withHoverEvent(HoverEvent(
                    HoverEvent.Action.SHOW_ITEM,
                    HoverEvent.ItemStackInfo(pokeGlance.pokemonItemStack)
                ))
            }
            root.append(hoverableName)

            if (after.isNotEmpty()) root.append(white(after))
        } else {
            root.append(white(mainText))
        }

        // 4. Append the hyperlinked coordinate component at the very end
        if (coordSuffixComponent != null) {
            root.append(coordSuffixComponent)
        }

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