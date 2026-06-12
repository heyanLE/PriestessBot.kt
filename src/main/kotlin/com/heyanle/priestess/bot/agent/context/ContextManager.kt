package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.agent.Agent
import com.heyanle.priestess.bot.provider.model.ConversationMessage

class ContextManager(
    private val tokenCounter: TokenCounter,
) {
    private val strategies: Map<String, ContextCompressStrategy> = mapOf(
        "round_truncation" to RoundTruncationStrategy(),
        "token_window" to TokenWindowStrategy(tokenCounter),
        "llm_compress" to LLMCompressStrategy(),
    )

    suspend fun compress(
        agent: Agent,
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int = -1,
        maxRounds: Int = -1,
    ): List<ConversationMessage> {
        if (messages.isEmpty()) return messages
        val effectiveMaxRounds = if (maxRounds > 0) maxRounds else 20
        val effectiveMaxTokens = if (maxTokens > 0) maxTokens else 8000
        if (!needsCompression(messages, effectiveMaxTokens, effectiveMaxRounds)) {
            return messages
        }
        val strategy = getStrategy(agent)
        return strategy.compress(messages, systemMessage, effectiveMaxTokens, effectiveMaxRounds)
    }

    fun needsCompression(
        messages: List<ConversationMessage>,
        maxTokens: Int,
        maxRounds: Int,
    ): Boolean {
        val roundCount = messages.count { it.role == "user" }
        if (roundCount > maxRounds) return true
        val totalTokens = tokenCounter.countAll(messages)
        if (totalTokens > maxTokens) return true
        return false
    }

    private fun getStrategy(agent: Agent): ContextCompressStrategy {
        return strategies[agent.compressStrategy] ?: strategies["round_truncation"]!!
    }
}
