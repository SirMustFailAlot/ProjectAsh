package io.github.sirmustfailalot

import net.minecraft.server.MinecraftServer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import org.slf4j.LoggerFactory

data class LabelDef(
    val display: String,
    val color: Int,      // 0xRRGGBB
    val bold: Boolean = true,
)

private val LABELS: Map<String, LabelDef> = mapOf(
    "shiny"        to LabelDef(display = "Shiny",       color = 0xF1C40F),
    "legendary"    to LabelDef(display = "Legendary",   color = 0x2ECC71),
    "ultra-beast"  to LabelDef(display = "Ultra-Beast", color = 0x3498DB),
    "projectash"  to LabelDef(display = "Project Ash", color = 0x0D1C6F)
)

private fun colored(text: String, rgb: Int, bold: Boolean = false): MutableComponent {
    val style = Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)
    return Component.literal(text).withStyle(style)
}

private fun white(text: String): MutableComponent {
    val style = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(false)
    return Component.literal(text).withStyle(style)
}

private fun normalizeLabel(s: String): String =
    s.lowercase().filter { it.isLetterOrDigit() }

object Announcement {
    private val logger = LoggerFactory.getLogger("ProjectAsh")

    fun spawn(server: MinecraftServer?, dimension: String, playerName: String?, spawnType: List<String>, species: String, posValue: String) {
        val ingameEnabled = Config.data.in_game.enabled
        if (ingameEnabled) {
            val message = if (dimension == "Overworld") {
                renderLabeledMessage(
                    labelsInOrder = spawnType,
                    messageTail = "$species spawned near $playerName at $posValue")
            } else {
                renderLabeledMessage(
                    labelsInOrder = spawnType,
                    messageTail = "$species spawned in the $dimension near $playerName at $posValue")
            }

            server.let { server ->
                server?.playerList?.players?.forEach { p ->
                    p.sendSystemMessage(message)
                }
            }
        }
    }

    fun capture(server: MinecraftServer?, playerName: String?, spawnType: List<String>, species: String) {
        val ingameEnabled = Config.data.in_game.enabled
        if (ingameEnabled) {
            val message = renderLabeledMessage(
                labelsInOrder = spawnType,
                messageTail = "$species was caught by $playerName!")

            server.let { server ->
                server?.playerList?.players?.forEach { p ->
                    p.sendSystemMessage(message)
                }
            }
        }
    }

    fun fainted(server: MinecraftServer?, spawnType: List<String>, species: String) {
        val ingameEnabled = Config.data.in_game.enabled
        if (ingameEnabled) {
            val message = renderLabeledMessage(
                labelsInOrder = spawnType,
                messageTail = "$species fainted! Well... Back to it then! :(")

            server.let { server ->
                server?.playerList?.players?.forEach { p ->
                    p.sendSystemMessage(message)
                }
            }
        }
    }

    fun discordWebhookFail(server: MinecraftServer?) {
        logger.info("Project Ash: discord_webhook missing in config; skipping webhook send.")
        val message = renderLabeledMessage(
            labelsInOrder = listOf("ProjectAsh"),
            messageTail = "Failed to send discord webhook!")

        server.let { server ->
            server?.playerList?.players?.forEach { p ->
                p.sendSystemMessage(message)
            }
        }
    }

    fun renderLabeledMessage(
        labelsInOrder: List<String>,
        messageTail: String,
        separator: String = " · "
    ): MutableComponent {
        val parts = mutableListOf<MutableComponent>()

        // 1) Build colored label components
        val labelComps = labelsInOrder
            .map { normalizeLabel(it) }
            .distinct()
            .mapNotNull { key -> LABELS[key]?.let { def -> colored(def.display, def.color, def.bold) } }

        // 2) Add them with separators
        if (labelComps.isNotEmpty()) {
            parts += labelComps.first()
            for (i in 1 until labelComps.size) {
                parts += white(separator)
                parts += labelComps[i]
            }
            parts += white(" ") // space before main message
        }

        // 3) Append the message tail (white)
        parts += white(messageTail)

        // 4) Fold all components together
        return parts.drop(1).fold(parts.first()) { acc, nxt -> acc.append(nxt) }
    }
}