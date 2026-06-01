package io.github.sirmustfailalot.projectash.pipeline

// Java Classes
import java.util.UUID

class DiscordCriteria {
    var isServerAllowedSpawn: Boolean = false
    var isServerMessage: Boolean = false
    val serverLabels = mutableListOf<String>()
    val serverRules = mutableListOf<String>()
}

class InGamePlayerCriteria {
    val finalLabels = mutableSetOf<String>()
}

class RuleEvaluationResult {
    val discordCriteria = DiscordCriteria()
    val playerCriteria = mutableMapOf<String, InGamePlayerCriteria>()
}