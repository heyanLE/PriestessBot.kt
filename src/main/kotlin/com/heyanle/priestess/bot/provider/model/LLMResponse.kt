package com.heyanle.priestess.bot.provider.model

import kotlinx.serialization.Serializable

@Serializable
data class LLMResponse(
    val content: String = "",
    val toolCalls: List<ToolCall> = emptyList(),
    val finishReason: String = "",
    val tokenUsage: TokenUsage = TokenUsage(),
) {
    fun hasToolCalls(): Boolean = toolCalls.isNotEmpty()
}

@Serializable
data class ToolCall(
    val id: String = "",
    val name: String = "",
    val arguments: String = "", // JSON string
)

@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)
