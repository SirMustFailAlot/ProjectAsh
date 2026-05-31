package io.github.sirmustfailalot.projectash.announcer

import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.pipeline.PokeStream
import io.github.sirmustfailalot.projectash.pipeline.RuleEvaluationResult
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.time.Instant

object SpwaningAnnouncer {
    private val logger = LoggerFactory.getLogger("ProjectAsh")
    private val gson = Gson()

    fun announceSpawn(pokeGlance: PokeStream.PokemonLifespan, announceDetails: RuleEvaluationResult) {
        // Discord Logic
        if (announceDetails.discordCriteria.isServerMessage && Config.data.server.discordEnabled) {
            val webhook = Config.data.server.discordWebhook
            if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                logger.info("Project Ash: Discord webhook not configured, skipping announcement")
                return
            }

            val serverLabels = announceDetails.discordCriteria.serverLabels
            val cleanLabelStr = UtilityAnnouncer.organiseLabelsToString(serverLabels)
            val title = (if (serverLabels.contains("Shiny")) "✨ " else "") + "Spawn - $cleanLabelStr - ${pokeGlance.speciesWithForm}"

            val fields = mutableListOf(
                EmbedField("Poke Tags", UtilityAnnouncer.organiseLabels(serverLabels).joinToString(", ")),
                EmbedField("Dimension", pokeGlance.spawnDimension),
                EmbedField("Closest Player", pokeGlance.spawnClosestPlayer),
                EmbedField("Position", "`${pokeGlance.spawnPos}`")
            )
            if (pokeGlance.spawnSource == "Unknown") {
                fields.add(0, EmbedField("Spawn Source", "Unknown"))
            }

            val rulesList = announceDetails.discordCriteria.serverRules
            val rulesString = if (rulesList.isNotEmpty()) " | " + rulesList.joinToString(" | ") else ""

            val embed = Embed(
                title = title,
                color = UtilityAnnouncer.getEmbedColour(serverLabels),
                fields = fields,
                thumbnail = pokeGlance.sprite?.let { mapOf("url" to it) },
                footer = mapOf("text" to "ProjectAsh$rulesString"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            DeliveryAnnouncer.discord(messageBody = body)
        }

        // In-game Logic
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
                // Uses the global utility method flawlessly
                val ingameMessage = UtilityAnnouncer.renderLabeledMessage(
                    notification.finalLabels.toList(),
                    messageText
                )
                DeliveryAnnouncer.ingame(playerName = playerName, messageBody = ingameMessage)
            }
        }
    }

    fun announceCapture(
        caughtBy: String,
        pokeGlance: PokeStream.PokemonLifespan,
        announceDetails: RuleEvaluationResult
    ) {
        // Discord Logic
        if (announceDetails.discordCriteria.isServerMessage && Config.data.server.discordEnabled) {
            val webhook = Config.data.server.discordWebhook
            if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                logger.info("Project Ash: Discord webhook not configured, skipping announcement")
                return
            }
            val serverLabels = announceDetails.discordCriteria.serverLabels
            val cleanLabelStr = UtilityAnnouncer.organiseLabelsToString(serverLabels)
            val title = "✅ Captured - $cleanLabelStr - ${pokeGlance.speciesWithForm}"

            val fields = mutableListOf(
                EmbedField("Poke Tags", cleanLabelStr),
                EmbedField("Caught By", caughtBy)
            )
            if (pokeGlance.spawnSource == "Unknown") {
                fields.add(0, EmbedField("Spawn Source", "Unknown"))
            }

            val rulesList = announceDetails.discordCriteria.serverRules
            val rulesString = if (rulesList.isNotEmpty()) " | " + rulesList.joinToString(" | ") else ""

            val embed = Embed(
                title = title,
                color = UtilityAnnouncer.getEmbedColour(serverLabels),
                fields = fields,
                thumbnail = pokeGlance.sprite?.let { mapOf("url" to it) },
                footer = mapOf("text" to "ProjectAsh$rulesString"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            DeliveryAnnouncer.discord(messageBody = body)
        }

        // In-game Logic
        if (announceDetails.playerCriteria.isNotEmpty()) {
            val messageText = "${pokeGlance.speciesWithForm} was caught by $caughtBy!"

            announceDetails.playerCriteria.forEach { (playerName, notification) ->
                // Uses the global utility method flawlessly
                val ingameMessage = UtilityAnnouncer.renderLabeledMessage(
                    notification.finalLabels.toList(),
                    messageText
                )
                DeliveryAnnouncer.ingame(playerName = playerName, messageBody = ingameMessage)
            }
        }
    }

    fun announceFainted(
        pokeGlance: PokeStream.PokemonLifespan,
        announceDetails: RuleEvaluationResult
    ) {
        // Discord Logic
        if (announceDetails.discordCriteria.isServerMessage && Config.data.server.discordEnabled) {
            val webhook = Config.data.server.discordWebhook
            if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                logger.info("Project Ash: Discord webhook not configured, skipping announcement")
                return
            }
            val serverLabels = announceDetails.discordCriteria.serverLabels
            val cleanLabelStr = UtilityAnnouncer.organiseLabelsToString(serverLabels)
            val title = "❌ Fainted - $cleanLabelStr - ${pokeGlance.speciesWithForm}"

            val fields = mutableListOf(
                EmbedField("Poke Tags", cleanLabelStr),
            )
            if (pokeGlance.spawnSource == "Unknown") {
                fields.add(0, EmbedField("Spawn Source", "Unknown"))
            }

            val rulesList = announceDetails.discordCriteria.serverRules
            val rulesString = if (rulesList.isNotEmpty()) " | " + rulesList.joinToString(" | ") else ""

            val embed = Embed(
                title = title,
                color = UtilityAnnouncer.getEmbedColour(serverLabels),
                fields = fields,
                thumbnail = mapOf("url" to "https://s-media-cache-ak0.pinimg.com/600x315/b1/20/08/b120087f3a904bda147251beaedf5755.jpg"),
                footer = mapOf("text" to "ProjectAsh$rulesString"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            DeliveryAnnouncer.discord(messageBody = body)
        }

        // In-game Logic
        if (announceDetails.playerCriteria.isNotEmpty()) {
            val messageText = "${pokeGlance.speciesWithForm} Fainted...... Well..... Back to it then!"

            announceDetails.playerCriteria.forEach { (playerName, notification) ->
                // Uses the global utility method flawlessly
                val ingameMessage = UtilityAnnouncer.renderLabeledMessage(
                    notification.finalLabels.toList(),
                    messageText
                )
                DeliveryAnnouncer.ingame(playerName = playerName, messageBody = ingameMessage)
            }
        }
    }

    fun announceRemoved(
        removalReason: String,
        pokeGlance: PokeStream.PokemonLifespan,
        announceDetails: RuleEvaluationResult
    ) {
        // Discord Logic
        if (announceDetails.discordCriteria.isServerMessage && Config.data.server.discordEnabled) {
            val webhook = Config.data.server.discordWebhook
            if (webhook.isNullOrBlank() || webhook == "https://your.webhook.url/here") {
                logger.info("Project Ash: Discord webhook not configured, skipping announcement")
                return
            }
            val serverLabels = announceDetails.discordCriteria.serverLabels
            val cleanLabelStr = UtilityAnnouncer.organiseLabelsToString(serverLabels)
            val title = "❌ Removed - $cleanLabelStr - ${pokeGlance.speciesWithForm}"

            val fields = mutableListOf(
                EmbedField("Poke Tags", cleanLabelStr),
                EmbedField("Removal Reason", removalReason)
            )
            if (pokeGlance.spawnSource == "Unknown") {
                fields.add(0, EmbedField("Spawn Source", "Unknown"))
            }

            val rulesList = announceDetails.discordCriteria.serverRules
            val rulesString = if (rulesList.isNotEmpty()) " | " + rulesList.joinToString(" | ") else ""

            val embed = Embed(
                title = title,
                color = UtilityAnnouncer.getEmbedColour(serverLabels),
                fields = fields,
                thumbnail = mapOf("url" to "https://i.pinimg.com/originals/a9/48/e0/a948e0a1af81e162fe766faeeba3bc51.jpg"),
                footer = mapOf("text" to "ProjectAsh$rulesString"),
                timestamp = Instant.now().toString()
            )

            val body = gson.toJson(WebhookPayload(embeds = listOf(embed)))
            DeliveryAnnouncer.discord(messageBody = body)
        }

        // In-game Logic
        if (announceDetails.playerCriteria.isNotEmpty()) {
            val messageText = "${pokeGlance.speciesWithForm} Removed (${removalReason})"

            announceDetails.playerCriteria.forEach { (playerName, notification) ->
                // Uses the global utility method flawlessly
                val ingameMessage = UtilityAnnouncer.renderLabeledMessage(
                    notification.finalLabels.toList(),
                    messageText
                )
                DeliveryAnnouncer.ingame(playerName = playerName, messageBody = ingameMessage)
            }
        }
    }
}