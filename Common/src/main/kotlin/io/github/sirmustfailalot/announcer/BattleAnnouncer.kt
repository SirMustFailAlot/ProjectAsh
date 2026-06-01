package io.github.sirmustfailalot.projectash.announcer

import io.github.sirmustfailalot.projectash.pipeline.BattleStream
import io.github.sirmustfailalot.projectash.config.Config
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import com.google.gson.Gson
import java.time.Instant

object BattleAnnouncer {
    private val gson = Gson()

    fun announceBattleStart(server: MinecraftServer, glance: BattleStream.BattleGlance) {
        val inGameText = if (glance.isPvP) {
            "⚔️ §bPvP Battle Started: §f${glance.playerNames} §7vs §f${glance.opponentNames}"
        } else {
            "§eBattle Started: §f${glance.playerNames} §7vs §f${glance.opponentNames}"
        }
        server.playerList.players.forEach { p -> p.sendSystemMessage(Component.literal(inGameText)) }

        if (Config.data.server.discordEnabled) {
            val title = if (glance.isPvP) "⚔️ PvP Battle Started!" else "🎒 Trainer Challenge Initiated!"

            val fields = listOf(
                EmbedField(
                    name = "Matchup",
                    value = "**${glance.playerNames}** vs **${glance.opponentNames}**",
                    inline = false
                )
            )

            val embed = Embed(
                title = title,
                color = if (glance.isPvP) { 0xFF5555 } else { 0xFFAA00 },
                fields = fields,
                thumbnail = null,
                footer = mapOf("text" to "ProjectAsh · Battle Tracker"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            DeliveryAnnouncer.discord(messageBody = body)
        }
    }

    fun announceBattleVictory(server: MinecraftServer, summary: BattleStream.BattleSummaryGlance) {
        val winners = summary.participants.filter { it.isWinner }.joinToString(", ") { "§a" + it.displayName }
        val losers = summary.participants.filter { !it.isWinner }.joinToString(", ") { "§c" + it.displayName }
        val inGameSummaryText = "§eBattle Finished: $winners §7vs $losers"
        server.playerList.players.forEach { p -> p.sendSystemMessage(Component.literal(inGameSummaryText)) }

        if (Config.data.server.discordEnabled) {

            val fields = summary.participants.map { participant ->
                val outcomeTag = if (participant.isWinner) "🏆 WINNER" else "💀 DEFEAT"

                val partyLines = participant.teams.joinToString("\n") { poke ->
                    if (poke.name == "???") {
                        "🟪 *${poke.name}*"
                    } else {
                        val statusEmoji = if (poke.isFainted) "🟥" else "🟩"
                        val shinySparkle = if (poke.isShiny) "✨ " else ""
                        "$statusEmoji $shinySparkle${poke.name} *(Lv. ${poke.level})*"
                    }
                }.ifBlank { "*No Pokémon brought*" }

                EmbedField(
                    name = "${participant.displayName}\n($outcomeTag)",
                    value = partyLines,
                    inline = true
                )
            }

            val teamWonAll = summary.participants.any { it.isWinner }
            val cardColor = if (teamWonAll) { 0x2ECC71 } else { 0xE74C3C }

            val embed = Embed(
                title = "⚔️ Trainer Battle Encounter Finished",
                color = cardColor,
                fields = fields,
                thumbnail = null,
                footer = mapOf("text" to "ProjectAsh · Battle Tracker"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            DeliveryAnnouncer.discord(messageBody = body)
        }
    }
}