package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.tool.ToolResult

sealed class AgentResponse {
    data class Thinking(val content: String) : AgentResponse()

    data class ToolResult(
        val toolCallId: String,
        val toolName: String,
        val toolResult: com.heyanle.priestess.bot.tool.ToolResult,
    ) : AgentResponse()

    data class Final(val content: String) : AgentResponse()

    data class Error(val message: String, val cause: Throwable? = null) : AgentResponse()
}
