package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

/**
 * LLM 压缩策略占位实现，当前回退到 token 窗口压缩。
 */
class LLMCompressStrategy(
    tokenCounter: TokenCounter,
) : ContextCompressStrategy {
    override val name: String = "llm_compress"
    private val fallback = TokenWindowStrategy(tokenCounter)

    override suspend fun compress(
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int,
        maxRounds: Int,
    ): List<ConversationMessage> {
        return fallback.compress(messages, systemMessage, maxTokens, maxRounds)
    }
}
