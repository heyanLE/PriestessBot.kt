package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

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
