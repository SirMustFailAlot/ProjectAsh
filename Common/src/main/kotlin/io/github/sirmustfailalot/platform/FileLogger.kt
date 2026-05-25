package io.github.sirmustfailalot.utility

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FileLogger {
    var platformImpl: PlatformLogger? = null
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Synchronized
    fun log(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val formattedLine = "[$timestamp] $message"

        platformImpl?.logToFile(formattedLine) ?: println("[Fallback Core Log] $formattedLine")
    }
}