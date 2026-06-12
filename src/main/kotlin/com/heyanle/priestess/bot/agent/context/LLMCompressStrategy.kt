package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

class LLMCompressStrategy : ContextCompressStrategy {
    override val name: String = "llm_compress"

    override suspend fun compress(
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int,
        maxRounds: Int,
    ): List<ConversationMessage> {
        throw NotImplementedError("LLMCompressStrategy is reserved for v2")
    }
}
