package io.github.sirmustfailalot

// Project Ash - Subscribers
import io.github.sirmustfailalot.projectash.subscribers.EventSubscribers

// Logger
import org.slf4j.LoggerFactory

object ProjectAsh {
    private val logger = LoggerFactory.getLogger("ProjectAsh")

    fun initialise() {
        logger.info("Project Ash Initialising..... *Taps Mic*..... is this thing on?..... Boogies.")
        EventSubscribers.startSubscribers()
    }
}