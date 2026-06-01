package io.github.sirmustfailalot

// Project Ash Classes
import io.github.sirmustfailalot.projectash.config.Config
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers

// General Logger
import org.slf4j.LoggerFactory

object ProjectAsh {
    private val logger = LoggerFactory.getLogger("ProjectAsh")

    fun initialise() {
        logger.info("Project Ash Initialising..... *Taps Mic*..... is this thing on?..... Boogies.")
        Config.init()
        EventSubscribers.startSubscribers()
    }
}