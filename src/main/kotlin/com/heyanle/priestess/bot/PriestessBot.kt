package com.heyanle.priestess.bot

import com.heyanle.priestess.bot.core.di.coreModule
import com.heyanle.priestess.bot.platform.PlatformController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

suspend fun main() {
    val logger = KotlinLogging.logger("PriestessBot")
    logger.info { "PriestessBot starting..." }

    val app = startKoin {
        modules(coreModule)
    }

    val platformController = app.koin.get<PlatformController>()

    logger.info { "PriestessBot is running. Press Ctrl+C to stop." }

    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            runBlocking {
                platformController.stop()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during shutdown" }
        } finally {
            logger.info { "PriestessBot stopped." }
        }
    })

    awaitCancellation()
}
