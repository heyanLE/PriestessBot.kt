package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val enabled: Boolean = false,
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val corsEnabled: Boolean = true,
    val configWatchEnabled: Boolean = false,
    val configWatchIntervalMillis: Long = 2_000,
    val apiToken: String = "",
)
