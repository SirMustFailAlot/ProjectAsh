package io.github.sirmustfailalot

import io.github.sirmustfailalot.utility.FileLogger
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
    "catchem!"  to LabelDef(display = "CatchEm!", color = 0xD35400),
    "gotEm!"  to LabelDef(display = "GotEm!", color = 0x27AE60),
    "perfect"  to LabelDef(display = "Perfect", color = 0x3498DB),
    "perfect"  to LabelDef(display = "Perfect", color = 0x3498DB),
    "shiny"        to LabelDef(display = "Shiny",       color = 0xF1C40F),
    "legendary"    to LabelDef(display = "Legendary",   color = 0x2ECC71),
    "mythical"  to LabelDef(display = "Mythical", color = 0x9B59B6),
    "ultra_beast"  to LabelDef(display = "Ultra Beast", color = 0xE74C3C),
    "paradox"  to LabelDef(display = "Paradox", color = 0x95A5A6),
    "special"  to LabelDef(display = "Special", color = 0xE67E22),
    "projectash"  to LabelDef(display = "Project Ash", color = 0x1ABC9C),
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

    fun spawn(announceSource: String, announceTarget: String, catchEmAllPlayers: List<String>, announcePlayers: List<String>, server: MinecraftServer?, dimension: String, playerName: String?, spawnType: List<String>, species: String, posValue: String) {
        val message = if (dimension == "Overworld") {
            if (announceSource === "Unknown") {
                renderLabeledMessage(
                    labelsInOrder = spawnType,
                    messageTail = "$species has somehow spawned near $playerName at $posValue"
                )
            } else {
                renderLabeledMessage(
                    labelsInOrder = spawnType,
                    messageTail = "$species spawned near $playerName at $posValue"
                )
            }
        } else {
            if (announceSource === "Unknown") {
                renderLabeledMessage(
                    labelsInOrder = spawnType,
                    messageTail = "$species has somehow spawned in the $dimension, near $playerName at $posValue"
                )
            } else {
                renderLabeledMessage(
                    labelsInOrder = spawnType,
                    messageTail = "$species spawned in the $dimension, near $playerName at $posValue"
                )
            }
        }

        val nonCatchEmAllPlayers = announcePlayers.filter { it !in catchEmAllPlayers }
        if (announceTarget == "Server") {
            val ingameEnabled = Config.data.server.ingameEnabled
            if (ingameEnabled) {
                server.let { server ->
                    server?.playerList?.players?.forEach { p ->
                        p.sendSystemMessage(message)
                    }
                }
            }
        } else if (announceTarget == "Players" && announceSource != "Unknown") {
            if (server != null) sendMessageToPlayers(server, message, nonCatchEmAllPlayers)
        }
        val uncaughtMessage = prefixRenderLabeledMessage("catchem!", message=message)
        if (server != null) sendMessageToPlayers(server=server, message = uncaughtMessage, names=catchEmAllPlayers)
    }

    fun capture(announceTarget: String, announcePlayers: List<String>, server: MinecraftServer?, playerName: String?, spawnType: List<String>, species: String) {
        val message = renderLabeledMessage(
            labelsInOrder = spawnType,
            messageTail = "$species was caught by $playerName!")
        FileLogger.log(message.string)
        if (announceTarget == "Server") {
            val ingameEnabled = Config.data.server.ingameEnabled
            if (ingameEnabled) {
                server.let { server ->
                    server?.playerList?.players?.forEach { p ->
                        p.sendSystemMessage(message)
                    }
                }
            }
        } else if (announceTarget == "Players") {
            if (server != null) sendMessageToPlayers(server, message, announcePlayers)
        }
    }

    fun fainted(announceTarget: String, announcePlayers: List<String>, server: MinecraftServer?, spawnType: List<String>, species: String) {
        val message = renderLabeledMessage(
            labelsInOrder = spawnType,
            messageTail = "$species fainted! Well... Back to it then! :(")
        FileLogger.log(message.string)
        if (announceTarget == "Server") {
            val ingameEnabled = Config.data.server.ingameEnabled
            if (ingameEnabled) {
                server.let { server ->
                    server?.playerList?.players?.forEach { p ->
                        p.sendSystemMessage(message)
                    }
                }
            }
        } else if (announceTarget == "Players") {
            if (server != null) sendMessageToPlayers(server, message, announcePlayers)
        }
    }

    fun removed(announceTarget: String, announcePlayers: List<String>, server: MinecraftServer?, spawnType: List<String>, species: String) {
        val message = renderLabeledMessage(
            labelsInOrder = spawnType,
            messageTail = "$species has despawned!")
        FileLogger.log(message.string)
        if (announceTarget == "Server") {
            val ingameEnabled = Config.data.server.ingameEnabled
            if (ingameEnabled) {
                server.let { server ->
                    server?.playerList?.players?.forEach { p ->
                        p.sendSystemMessage(message)
                    }
                }
            }
        } else if (announceTarget == "Players") {
            if (server != null) sendMessageToPlayers(server, message, announcePlayers)
        }
    }

    fun hatched( server: MinecraftServer?, hatchType: List<String>, species: String, playerName: String) {
        val message = renderLabeledMessage(
            labelsInOrder = hatchType,
            messageTail = "$species has been hatched by $playerName!")
        FileLogger.log(message.string)
        val ingameEnabled = Config.data.server.ingameEnabled
        if (ingameEnabled) {
            server.let { server ->
                server?.playerList?.players?.forEach { p ->
                    p.sendSystemMessage(message)
                }
            }
        }

    }

    fun sendMessageToPlayers(server: MinecraftServer, message: Component, names: List<String>) {
        val allowed = names.map { it.lowercase() }.toSet()
        server.playerList.players.forEach { player ->
            if (player.scoreboardName.lowercase() in allowed) {
                player.sendSystemMessage(message)
            }
        }
    }

    fun discordWebhookFail(server: MinecraftServer?) {
        logger.info("Project Ash: discord_webhook missing in config; skipping webhook send.")
        val message = renderLabeledMessage(
            labelsInOrder = listOf("ProjectAsh"),
            messageTail = "Failed to send discord webhook!")
        FileLogger.log(message.string)
        server.let { server ->
            server?.playerList?.players?.forEach { p ->
                p.sendSystemMessage(message)
            }
        }
    }

    fun prefixRenderLabeledMessage(
        prefix: String,
        message: MutableComponent
    ): MutableComponent {
        val root = Component.empty()
        val labelComps = prefix
            .map { prefix }
            .distinct()
            .mapNotNull { key -> LABELS[key]?.let { def -> colored(def.display, def.color, def.bold) } }

        root.append(labelComps.first())
        root.append(white(" "))
        root.append(message)
        return root
    }

    fun renderLabeledMessage(
        labelsInOrder: List<String>,
        messageTail: String,
        separator: String = " · "
    ): MutableComponent {
        // Start with a literal empty component root to avoid mutating your actual labels
        val root = Component.empty()

        // 1) Build colored label components
        val labelComps = labelsInOrder
            .map { normalizeLabel(it) }
            .distinct()
            .mapNotNull { key -> LABELS[key]?.let { def -> colored(def.display, def.color, def.bold) } }

        // 2) Add them with separators directly to the root
        if (labelComps.isNotEmpty()) {
            root.append(labelComps.first())
            for (i in 1 until labelComps.size) {
                root.append(white(separator))
                root.append(labelComps[i])
            }
            root.append(white(" "))
        }

        // 3) Append the message tail
        root.append(white(messageTail))

        return root
    }
}