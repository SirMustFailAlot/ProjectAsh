package io.github.sirmustfailalot.utility

import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class FabricLoggerImpl : PlatformLogger {
    private val logFile: File = FabricLoader.getInstance().gameDir.resolve("logs/ProjectAsh.log").toFile()

    init {
        if (!logFile.exists()) {
            logFile.parentFile.mkdirs()
            logFile.createNewFile()
        }
    }

    override fun logToFile(message: String) {
        try {
            val lineWithNewline = "$message\n"
            Files.write(
                logFile.toPath(),
                lineWithNewline.toByteArray(Charsets.UTF_8),
                StandardOpenOption.APPEND
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}