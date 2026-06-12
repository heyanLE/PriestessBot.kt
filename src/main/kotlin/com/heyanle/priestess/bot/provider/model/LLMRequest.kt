package com.heyanle.priestess.bot.provider.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LLMRequest(
    val model: String = "",
    val messages: List<ConversationMessage> = emptyList(),
    val tools: List<JsonObject> = emptyList(),
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val stream: Boolean = false,
)
