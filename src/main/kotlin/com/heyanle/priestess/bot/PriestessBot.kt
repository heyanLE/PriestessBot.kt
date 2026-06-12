package com.heyanle.priestess.bot

import com.heyanle.priestess.bot.core.di.coreModule
import com.heyanle.priestess.bot.core.lifecycle.CoreLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

suspend fun main() {
    println("PriestessBot starting...")

    val app = startKoin {
        modules(coreModule)
    }

    val lifecycle = app.koin.get<CoreLifecycle>()
    lifecycle.start()

    println("PriestessBot is running. Press Ctrl+C to stop.")

    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            runBlocking {
                lifecycle.stop()
            }
        } catch (e: Exception) {
            System.err.println("[PriestessBot] ERROR during shutdown: ${e.message}")
        } finally {
            println("PriestessBot stopped.")
        }
    })

    awaitCancellation()
}
