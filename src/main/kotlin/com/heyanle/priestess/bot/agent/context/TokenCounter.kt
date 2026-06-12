package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

class TokenCounter {

    fun count(message: ConversationMessage): Int {
        val roleOverhead = 4
        val contentTokens = estimateTokens(message.content)
        val toolCallTokens = message.toolCalls?.sumOf { tc ->
            estimateTokens(tc.name) + estimateTokens(tc.arguments)
        } ?: 0
        val toolCallIdTokens = message.toolCallId?.let { estimateTokens(it) } ?: 0
        val nameTokens = message.name?.let { estimateTokens(it) } ?: 0
        return roleOverhead + contentTokens + toolCallTokens + toolCallIdTokens + nameTokens
    }

    fun countAll(messages: List<ConversationMessage>): Int {
        if (messages.isEmpty()) return 0
        val requestOverhead = 2
        return requestOverhead + messages.sumOf { count(it) }
    }

    private fun estimateTokens(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        val totalChars = text.length
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' || it in '\u3400'..'\u4dbf' }
        val otherChars = totalChars - chineseChars
        val chineseTokens = chineseChars / 1.5
        val otherTokens = otherChars / 4.0
        return ((chineseTokens + otherTokens) / 2.0).toInt().coerceAtLeast(1)
    }
}
