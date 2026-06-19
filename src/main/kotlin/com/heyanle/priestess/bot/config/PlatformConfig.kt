package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class PlatformConfig(
    val name: String = "",
    val type: String = "",
    val enabled: Boolean = true,
    val host: String = "127.0.0.1",
    val port: Int = 3000,
    val wsPort: Int = 3001,
    val token: String = "",
    val baseUrl: String = "",
    val useWs: Boolean = true,
    val config: Map<String, String> = emptyMap(),
)
