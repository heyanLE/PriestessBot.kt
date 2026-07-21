package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.agent.Agent
import com.heyanle.priestess.bot.agent.CompressStrategy
import com.heyanle.priestess.bot.provider.model.ConversationMessage

/**
 * 上下文管理器，负责判断消息是否需要压缩并选择对应压缩策略。
 */
class ContextManager(
    private val tokenCounter: TokenCounter,
    private val strategies: Map<CompressStrategy, ContextCompressStrategy> = mapOf(
        CompressStrategy.ROUND_TRUNCATION to RoundTruncationStrategy(),
        CompressStrategy.TOKEN_WINDOW to TokenWindowStrategy(tokenCounter),
    ),
) {
    suspend fun compress(
        agent: Agent,
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int = -1,
        maxRounds: Int = -1,
    ): List<ConversationMessage> {
        if (messages.isEmpty()) return messages
        val validMessages = ToolCallRoundSanitizer.sanitize(messages)
        val effectiveMaxRounds = if (maxRounds > 0) maxRounds else agent.maxContextRounds
        val effectiveMaxTokens = if (maxTokens > 0) maxTokens else agent.maxContextTokens
        if (!needsCompression(validMessages, effectiveMaxTokens, effectiveMaxRounds)) {
            return validMessages
        }
        val strategy = getStrategy(agent)
        return ToolCallRoundSanitizer.sanitize(
            strategy.compress(validMessages, systemMessage, effectiveMaxTokens, effectiveMaxRounds),
        )
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
        return strategies[agent.compressStrategy]
            ?: error("Unknown compress strategy: ${agent.compressStrategy}")
    }
}
