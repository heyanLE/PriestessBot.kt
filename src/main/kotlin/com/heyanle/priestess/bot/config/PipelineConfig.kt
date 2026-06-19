package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class PipelineConfig(
    val wakingPrefix: String = "/",
    val whitelistEnabled: Boolean = false,
    val whitelistUsers: List<String> = emptyList(),
    val whitelistGroups: List<String> = emptyList(),
    val rateLimitEnabled: Boolean = true,
    val rateLimitPerMinute: Int = 20,
    val sessionEnabledByDefault: Boolean = true,
    val contentSafetyEnabled: Boolean = false,
    val maxHistoryMessages: Int = 50,
)
