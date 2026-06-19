package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val path: String = "data/priestess.db",
)
