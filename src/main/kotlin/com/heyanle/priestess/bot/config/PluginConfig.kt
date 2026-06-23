package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class PluginConfig(
    val enabled: Boolean = true,
    val directory: String = "plugins",
    val autoDiscover: Boolean = true,
)
