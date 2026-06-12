package com.heyanle.priestess.bot.conversation

data class Conversation(
    val id: String,
    val platform: String,
    val sessionId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
