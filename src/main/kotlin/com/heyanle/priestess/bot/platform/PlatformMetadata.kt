package com.heyanle.priestess.bot.platform

data class PlatformMetadata(
    val name: String,
    val displayName: String,
    val supportStreaming: Boolean,
    val supportProactiveMessage: Boolean,
)
